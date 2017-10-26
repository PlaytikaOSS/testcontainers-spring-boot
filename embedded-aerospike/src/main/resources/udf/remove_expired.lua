function remove_expired(rec, expireTime)
    if (record.ttl(rec) < expireTime) then
        aerospike:remove(rec)
    end
end