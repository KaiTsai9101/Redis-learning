package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;

import java.util.Collection;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SHOP_LIST_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        // 获取缓存数据
        String shopListJson = stringRedisTemplate.opsForValue().get(SHOP_LIST_KEY);

        // 如果缓存存在，则直接返回
        if (StrUtil.isNotBlank(shopListJson)) {
            List<ShopType> shopTypeList = JSONUtil.toList(shopListJson, ShopType.class);
            return Result.ok(shopTypeList);
        }

        // 如果缓存不存在，则查询数据库
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();

        // 如果数据库不存在，则返回错误
        if (CollectionUtil.isEmpty(shopTypeList)) {
            return Result.fail("店铺类型不存在");
        }

        // 将数据库的数据转换成Json存储到缓存中
        String jsonStr = JSONUtil.toJsonStr(shopTypeList);
        stringRedisTemplate.opsForValue().set(SHOP_LIST_KEY, jsonStr);

        // 返回
        return Result.ok(shopTypeList);
    }
}
