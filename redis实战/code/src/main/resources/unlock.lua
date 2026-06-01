-- 获取锁中的线程标示 get key
local id = redis.call('get', KEY[1])
-- 比较线程标示与锁中的标示是否一致
if(id == ARGV[1]) then
    -- 释放锁 del key
    redis.call('del', KEY[1])
    return true
end
return 0