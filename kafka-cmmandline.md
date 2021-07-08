```bash
kafka-topics \
 --bootstrap-server broker01:9092 \
 --create \
 --topic users \
 --partitions 4 \
 --replication-factor 1
```


```bash
kafka-topics \
 --bootstrap-server broker01:9092 \
 --describe \
 --topic users

```

```bash
kafka-console-producer \
 --bootstrap-server broker01:9092 \
 --property key.separator=, \
 --property parse.key=true \
 --topic users

```

```bash
kafka-console-consumer \
 --bootstrap-server localhost:9092 \
 --topic users \
 --from-beginning
```

```bash
./kafka-console-consumer.sh \
 --bootstrap-server broker01:9092 \
 --topic users \
 --property print.timestamp=true \
 --property print.key=true \
 --property print.value=true \
 --from-beginning
```