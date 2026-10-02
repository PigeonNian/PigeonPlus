param(
    [Parameter(Mandatory = $true)][string]$ErrFile,
    [string]$OutPrefix = "idx"
)

# 分析 javac 错误日志。
#
# 为什么不能逐行正则匹配：
#   1. 日志是混合编码，必须容错解码，否则 ReadAllText 整体失败。
#   2. PowerShell 包装 gradlew.bat 时会在约 80 列处折行，
#      连「文件路径」都可能被拆到下一行（`.java:25:` 断成两截）。
#   3. javac 自身输出中文错误消息时也会折行。
# 因此改为：先把整份日志的折行折叠掉，再在整串上做正则匹配。
#
# 注意脚本必须以 UTF-8 BOM 保存，否则 PowerShell 5.1 按 ANSI 解码，
# 脚本内的中文字面量（如「错误:」）会变成乱码而匹配不到。

$enc = New-Object System.Text.UTF8Encoding($false, $false)
$text = [System.IO.File]::ReadAllText($ErrFile, $enc)

# 规范化：把「换行 + 顶格」的续行接回上一行（源码行与插入符行是缩进的，保持原样）
$normalized = [regex]::Replace($text, "`r?`n(?=[^\s])", '')
$flatLines = $normalized -split "`r?`n"

$out = New-Object System.Collections.Generic.List[string]
foreach ($line in $flatLines) {
    $m = [regex]::Match($line, '([^\\/:]+\.java):(\d+): 错误: (.+)')
    if (-not $m.Success) { continue }
    $msg = $m.Groups[3].Value.Trim()
    # 去掉消息尾部可能附带的源码片段（两个以上空格之后的内容）
    $msg = ($msg -split '\s{2,}')[0].Trim()
    $sym = ''
    $sm = [regex]::Match($line, '符号:\s*(类|方法|变量)?\s*(\S+)')
    if ($sm.Success) { $sym = $sm.Groups[1].Value + $sm.Groups[2].Value }
    $out.Add($m.Groups[1].Value + '|' + $m.Groups[2].Value + '|' + $msg + '|' + $sym)
}

$uniq = @($out | Sort-Object -Unique)
$report = New-Object System.Collections.Generic.List[string]
$report.Add("UNIQUE_ERRORS=$($uniq.Count)")
$report.Add('')
$report.Add('## BY FILE')
foreach ($g in ($uniq | ForEach-Object { $_.Split('|')[0] } | Group-Object | Sort-Object Count -Descending)) {
    $report.Add("$($g.Count)`t$($g.Name)")
}
$report.Add('')
$report.Add('## BY MESSAGE')
foreach ($g in ($uniq | ForEach-Object { $_.Split('|')[2] } | Group-Object | Sort-Object Count -Descending | Select-Object -First 25)) {
    $report.Add("$($g.Count)`t$($g.Name)")
}
$report.Add('')
$report.Add('## ALL')
foreach ($row in $uniq) { $report.Add($row) }

$path = Join-Path $env:TEMP ("$OutPrefix" + ".txt")
[System.IO.File]::WriteAllLines($path, $report, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "UNIQUE_ERRORS=$($uniq.Count)"
Write-Host "written: $path"
