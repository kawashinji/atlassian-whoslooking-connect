web: sh bin/warmup & target/universal/stage/bin/whoslooking-connect ${JAVA_OPTS_PROCFILE} -Dhttp.port=$PORT -Devolutions.use.locks=true -DapplyEvolutions.default=true -Ddb.default.driver=org.postgresql.Driver -Ddb.default.url=${DATABASE_URL}?ssl=true\&sslfactory=org.postgresql.ssl.NonValidatingFactory -Dredis.uri=${REDISCLOUD_URL}  -Dconfig.resource=prod.conf -Dlogger.resource=prod-logger.xml -DapplyEvolutions.default=true -DPLAY_LOG_LEVEL=${PLAY_LOG_LEVEL} -DAPPLICATION_LOG_LEVEL=${APPLICATION_LOG_LEVEL} -DAC_LOG_LEVEL=${AC_LOG_LEVEL} -DAC_KEY=${AC_KEY} -DROOT_LOG_LEVEL=${ROOT_LOG_LEVEL}
