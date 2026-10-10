param(
    [string]$SourceRoot = "src/main/java/dev/anvilcraft/pigeonplus/mixin",
    [string]$OutFile = "$env:TEMP\mixin-check.txt"
)

# 校验 mixin 的注入目标方法是否真实存在。
#
# 为什么需要这个脚本：
#   Mixin 的 `method = "xxx"` 是**字符串**，编译期完全不校验。
#   目标方法若被改名/改签名，只有游戏启动时才会抛
#   InvalidInjectionException，且 FATAL 可能掩盖后续问题，
#   导致「改一个、启动一次」的低效循环。
#   本脚本用 javap 直接核对，一次列出全部失效目标。

$ErrorActionPreference = 'Continue'
$projectRoot = (Get-Location).Path

# 收集 classpath：MC merged jar + 各依赖 jar
$cpParts = New-Object System.Collections.Generic.List[string]
$mcJar = Join-Path $projectRoot "build\moddev\artifacts\minecraft-patched-26.1.2.115-merged.jar"
if (Test-Path $mcJar) { $cpParts.Add($mcJar) }

# AnvilCraft / AnvilLib 等依赖：从 gradle 缓存里挑最新的 26.1 版本
$depRoot = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1"
foreach ($g in @('dev.dubhe\anvilcraft-neoforge-26.1.2', 'dev.anvilcraft.lib\anvillib-neoforge-26.1',
                 'dev.anvilcraft.lib\anvillib-registrum-neoforge-26.1', 'dev.anvilcraft.resource\ageratum-neoforge-26.1.2',
                 'net.neoforged\neoforge')) {
    $p = Join-Path $depRoot $g
    if (-not (Test-Path $p)) { continue }
    # 取版本号最大的那个目录下的主 jar
    $jar = Get-ChildItem $p -Recurse -Filter *.jar -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources|javadoc' } |
        Sort-Object { $_.Directory.Parent.Name } -Descending |
        Select-Object -First 1
    if ($jar) { $cpParts.Add($jar.FullName) }
}
$cp = $cpParts -join ';'

$javap = "C:\Users\鸽の念\.jdks\jbr-25.0.3\bin\javap.exe"
if (-not (Test-Path $javap)) {
    # 退化：用 PATH 里的 javap
    $javap = "javap"
}

# 简单的类名解析：把 mixin 源码里的 import 与 @Mixin 目标对应起来
$report = New-Object System.Collections.Generic.List[string]
$report.Add("classpath entries: $($cpParts.Count)")
$report.Add("")

# 缓存 javap 结果，避免重复调用
$methodCache = @{}
function Get-Methods([string]$className) {
    if ($methodCache.ContainsKey($className)) { return $methodCache[$className] }
    # 用 cmd /c 调用，避免 javap 的 stderr 被 PowerShell 当成异常中止脚本；
    # 类名里的 $ 在变量中不会被二次展开，可安全传给 javap。
    $out = & cmd /c "`"$javap`" -p -cp `"$cp`" $className 2>&1"
    $text = ($out | Out-String)
    $methodCache[$className] = $text
    return $text
}

$problems = 0
foreach ($file in (Get-ChildItem $SourceRoot -Recurse -Filter *.java)) {
    $text = [System.IO.File]::ReadAllText($file.FullName, (New-Object System.Text.UTF8Encoding($false)))

    # 解析 @Mixin(...) 的目标：优先 targets="..."，否则取 Xxx.class
    $targetClass = $null
    $tm = [regex]::Match($text, '@Mixin\s*\(\s*targets\s*=\s*"([^"]+)"')
    if ($tm.Success) {
        $targetClass = $tm.Groups[1].Value.Replace('/', '.')
    } else {
        $tm = [regex]::Match($text, '@Mixin\s*\(\s*([A-Za-z0-9_.]+)\.class')
        if ($tm.Success) {
            $simple = $tm.Groups[1].Value
            # 从 import 里找出全限定名
            $im = [regex]::Match($text, '(?m)^import\s+([\w.]+\.' + [regex]::Escape($simple) + ');')
            if ($im.Success) { $targetClass = $im.Groups[1].Value }
            else { $targetClass = $simple }
        }
    }
    if (-not $targetClass) { continue }

    $methods = Get-Methods $targetClass
    if ($methods -match 'Error:|error:|找不到') {
        $report.Add("[$($file.Name)] 无法解析目标类 $targetClass")
        $problems++
        continue
    }

    # 逐个校验 method = "..."。
    #
    # 注意不能无条件按 ';' 切分：「a;b」是 Mixin 的「多目标」写法，
    # 但完整描述符（如 highlight(Lnet/...;Lnet/...;)Z）里的 ';' 是类型分隔符。
    # 早期版本无脑切分，会把描述符切成碎片并逐片误报「缺少方法」。
    # 判据：含 '(' 即为描述符，整体只取 '(' 之前的方法名。
    foreach ($mm in [regex]::Matches($text, 'method\s*=\s*"([^"]+)"')) {
        $methodSpec = $mm.Groups[1].Value
        $names = if ($methodSpec.Contains('(')) { @($methodSpec) } else { $methodSpec -split ';' }
        foreach ($name in $names) {
            $clean = ($name -split '\(')[0].Trim()
            if ($clean -match '^\$|lambda\$') { continue }
            # javap 输出里方法名后跟 '('
            if ($methods -notmatch ('(?m)\b' + [regex]::Escape($clean) + '\s*\(')) {
                $report.Add("[$($file.Name)] 目标 $targetClass 缺少方法: $clean")
                $problems++
            }
        }
    }

    # 校验 @Shadow / @Accessor 的目标成员。
    #
    # 这里有两个曾让本脚本「假通过」的坑：
    #   1. 正则括号必须配平。旧写法结尾是 '(?:=|;|\())'，多了一个右括号，
    #      .NET 解析期抛「Too many )'s」，整段循环被静默跳过。
    #   2. 必须按**终止符**区分方法与字段：以 '(' 结束的是方法，以 ';'/'=' 结束的才是字段。
    #      旧写法把方法也吃进来，再想靠事后匹配 '\(\s*\)' 补救是无效的
    #      ——匹配在 '(' 处就结束了，不含 ')'，于是每个 @Shadow 方法都被误报成「缺少字段」。
    foreach ($fm in [regex]::Matches($text, '@(?:Shadow|Accessor)\s*(?:\(\s*(?:value\s*=\s*)?"([^"]+)"\s*\))?\s*(?:[^\r\n;=()]*?)\b(\w+)\s*(=|;|\()')) {
        $memberName = $fm.Groups[2].Value
        $terminator = $fm.Groups[3].Value
        if ($fm.Groups[1].Success) {
            # @Accessor("xxx") 显式给出的是目标字段名
            $memberName = $fm.Groups[1].Value
            $isMethod = $false
        } else {
            $isMethod = ($terminator -eq '(')
        }
        if ($memberName -match '^(public|private|protected|static|final|abstract)$') { continue }

        if ($isMethod) {
            if ($methods -notmatch ('(?m)\b' + [regex]::Escape($memberName) + '\s*\(')) {
                $report.Add("[$($file.Name)] 目标 $targetClass 缺少方法（@Shadow）: $memberName")
                $problems++
            }
        } else {
            if ($methods -notmatch ('(?m)\b' + [regex]::Escape($memberName) + '\s*;')) {
                $report.Add("[$($file.Name)] 目标 $targetClass 缺少字段（@Shadow 不能解析继承字段）: $memberName")
                $problems++
            }
        }
    }
}

$report.Insert(0, "PROBLEMS=$problems")
[System.IO.File]::WriteAllLines($OutFile, $report, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "PROBLEMS=$problems"
Write-Host "written: $OutFile"
