web: target/universal/stage/bin/whoslooking-connect -Dhttp.port=$PORT -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL} -Dredis.uri=${REDISCLOUD_URL}  -Dconfig.resource=prod.conf -Dlogger.resource=prod-logger.xml -DapplyEvolutions.default=true -DPLAY_LOG_LEVEL=${PLAY_LOG_LEVEL} -DAPPLICATION_LOG_LEVEL=${APPLICATION_LOG_LEVEL} -DAC_LOG_LEVEL=trace -DAC_KEY=${AC_KEY}
