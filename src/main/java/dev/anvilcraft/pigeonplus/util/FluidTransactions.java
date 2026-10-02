package dev.anvilcraft.pigeonplus.util;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * 流体抽取的工具方法。
 *
 * <p>26.1 的流体能力改为「事务」模型：{@code extract(...)} 只是<b>登记</b>意图，
 * 必须在一棵已提交的事务里才会真正生效。直接调用而不开事务，
 * 会因「没有打开的事务」而失败或静默无效。
 *
 * <p>这里统一封装「开根事务 → 抽取 → 成功则提交」，避免每处都写一遍
 * try-with-resources。若抽取量为 0 则不提交（自动回滚，无副作用）。
 */
public final class FluidTransactions {
    private FluidTransactions() {
    }

    /**
     * 在独立事务里从指定槽位抽取。
     *
     * @return 实际抽取量；0 表示没有抽出任何东西
     */
    public static int extract(
        ResourceHandler<FluidResource> handler,
        int index,
        FluidResource resource,
        int amount
    ) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(index, resource, amount, transaction);
            if (extracted > 0) {
                transaction.commit();
            }
            return extracted;
        }
    }

    /**
     * 在独立事务里向指定槽位注入。
     *
     * @return 实际注入量；0 表示没有塞进去
     */
    public static int insert(
        ResourceHandler<FluidResource> handler,
        int index,
        FluidResource resource,
        int amount
    ) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(index, resource, amount, transaction);
            if (inserted > 0) {
                transaction.commit();
            }
            return inserted;
        }
    }
}
