package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.NonNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始");
        }
        // 3.判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 已经结束
            return Result.fail("秒杀已经结束");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足");
        }

        // 5.一人一单
        Long userId = UserHolder.getUser().getId();
//        // 锁放在这里而不是createVoucherOrder方法内
//        // 原因：方法执行完后就释放锁了，但此时数据由spring提交，期间如果其他线程获取锁，则无法查询到订单记录，从而导致并发安全问题（仍可重复购买优惠券）
//        synchronized(userId.toString().intern()) {
//            // 获取代理对象（事务）（Spring只能给代理对象创建事务Transactional，不能给目标对象this）
//            // AopContext.currentProxy() 的功能和getBean()一样，都是获取bean对象
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }
        // 创建锁对象
//        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLook = lock.tryLock();
        if (!isLook) {
            // 获取锁失败
            return Result.fail("不允许重复下单");
        }
        try {
            // 获取代理对象（事务）（Spring只能给代理对象创建事务Transactional，不能给目标对象this）
            // AopContext.currentProxy() 的功能和getBean()一样，都是获取bean对象
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 锁库存（由于toString()是将userId转换成全新对象，所以这里还需要intern()将该对象转换成与userId一样的值（返回字符串对象的规范表示））

        // 5.1.查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2.判断是否存在
        if (count > 0) {
            // 用户已经购买过了
            return Result.fail("用户已经购买过一次");
        }
        // 6.扣减库存（MyBatisPlus的条件构造器）
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")        // set stock = stock - 1
                .eq("voucher_id", voucherId)        // where voucher_id = ?
                .gt("stock", 0)         // and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            return Result.fail("库存不足");
        }
        // 7.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();// 7.1.订单ID
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 7.2.用户ID
        voucherOrder.setUserId(userId);
        // 7.3.代金券ID
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        // 8.返回订单结果
        return Result.ok(orderId);
    }
}
