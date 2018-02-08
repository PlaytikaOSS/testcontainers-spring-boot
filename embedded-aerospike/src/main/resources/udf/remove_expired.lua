function remove_expired(rec, expireTime)
    local recordTtl = record.ttl(rec)
    if (recordTtl > 0 and recordTtl < expireTime) then
        aerospike:remove(rec)
    end
end