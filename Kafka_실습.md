Kafka 실습
## 기본정보
1. kaf1 public - 3.20.238.248
2. kaf2 18.188.11.200
3. kaf3 52.14.217.250

## 카프카 세대 클러스터링

3.20.238.248 broker01
18.188.11.200 broker02 
52.14.217.250 broker03

tickTime=2000
dataDir=/var/lib/zookeeper
clientPort=2181
initLimit=20
syncLimit=5
server.1=broker01:2888:3888
server.2=broker02:2888:3888
server.3=broker03:2888:3888

상세: https://blog.voidmainvoid.net/325

broker.id=0
listeners=PLAINTEXT://:9092
advertised.listeners=PLAINTEXT://broker01:9092
zookeeper.connect=broker01:2181,broker02:2181,broker03/test

---

# 2.카프카 빠르게 시작해보기

## Ec2 접근법
```bash
cd /Users/user/Documents/dev/kaf
chmod 400 edgar-kaf1.pem.  //파일을 I 명령어로 읽어오기위해 유저권한변경
ssh -i edgar-kaf1.pem ec2-user@3.20.238.248
```


## 자바 설치
```bash
sudo yum install -y java-1.8.0-openjdk-devel.x86_64
java -version
```

## 주키퍼 카프카 브로커 실행
```bash
wget https://archive.apache.org/dist/kafka/2.7.0/kafka_2.12-2.7.0.tgz
tar xvf kafka_2.12-2.7.0.tgz
```


## 카프카 브로커 힙 메모리 설정
1. 카프카 브로커는 레코드의 내용은 페이지 캐시로 시스템메모리를 사용하고 나머지 객체들은 힙메모리에 저장 이러한특징으로 힙메모리를 5GB이상으로 사용하지 않음
2. 카프카 패키지의 힙메모리는 카프카 브로커는 1G 주키퍼는 512mb 로 기본설정 됨 t2.micro에서 두개를 같이 실행하면 1.5gb이므로 에러가남.
```bash
	export KAFKA_HEAP_OPTS=“-Xmx400m -Xms400m”
	echo $KAFKA_HEAP_OPTS
```

3. 터미널에 사용자가 입력한 환경변수는 터미널 세션이 종료되고나면 다시 초기화되어 재사용 불가능, 이를 해결하기위해 환경변수 선언문을 ~/.bashrc 파일에 넣어주면됨 ec2는 bash 쉘이라고 불리는 유닉스 쉘을 사용하는데, ~/.bashrc 파일은 bash 쉘이 실행될 때마다 반복적으로 구동되어 적용되는 파일이다.

```bash
	vi ~/.bashrc 
	## 아래구문 추가
	export KAFKA_HEAP_OPTS="-Xmx400m -Xms400m"
	$ source ~/.bashrc
	$ echo $KAFKA_HEAP_OPTS
```


## 카프카 브로커 실행 옵션 설정
```bash
vi config/server.properties
```

## 주키퍼 실행 및 로그확인
```bash
bin/zookeeper-server-start.sh -daemon config/zookeeper.properties
jps -vm
```


## 카프카 실행및 로그 확인 (kafka advertiesed.listeners)
```bash
bin/kafka-server-start.sh -daemon config/server.properties
jps -m
tail -f logs/server.log
```

## 2.1.5 로컬 컴퓨터에서 카프카와 통신 확인
- 로컬 컴퓨터에 카프카 바이너리패키지를 다운로드받아 kafaka-broke-api-version.sh 명령어를 로컬에서 실행해보자
```bash
curl https://archive.apache.org/dist/kafka/2.7.0/kafka_2.12-2.7.0.tgz --output kafka.tgz
tar -xvf kafka.tgz
bin/kafka-broker-api-versions.sh --bootstrap-server broker01:9092
```

## 테스트 편의를 위한 hosts 설정
```bash
sudo vi /etc/hosts 
broker01
```

## 2.2.1 kafka-topics.sh
- 이 명령어를 통해 토픽과 관련된 명령어 실행가능
- 토픽이란 데이터를 구분하는 가장 기본개념 (rdb의 테이블과 유사)
- 토픽에는 파티션이 존재하는데 최소 1개이상이다.
- TIP 토픽을 생성하는 2가지 방법
  1. 카프카 컨슈머 또는 프로듀서가 카프카 브로커에 생성되지 않은 토픽에 대해 데이터를 요청
  2. 두 번째는 커맨드 라인 툴로 명시적으로 토픽을 생성
  - 토픽을 명시적으로 생성하는것을 추천한다. 토픽마다 처리되어야하는 데이터 특성이 다르기 때문이다.
  - ex) 동시처리량이 많은 데이터는 파티션의 개수를 100으로 설정가능, 단기간 데이터 처리만 필요한경우는 보관기간을 짧게 설정
  - 위와같은 이유로 토픽에 들어오는 데이터 양과 병렬로 처리되어야하는 용량을 잘 파악하여 생성하는 것이 중요하다.

## 토픽 생성
```bash
bin/kafka-topics.sh --create --bootstrap-server broker01:9092 --topic hello.kafka
## config를통해 kafka-topics.sh 명령에 포함되지 않은 추가적인 설정을 할 수 있다. retention으로 2일이지난 데이터는 삭제된다.
bin/kafka-topics.sh --create --bootstrap-server broker01:9092 --partitions 3 --replication-factor 1 --config retention.ms=172800000 --topic hello.kafka.2
## 생성된 토픽확인
bin/kafka-topics.sh --bootstrap-server broker01:9092 --list
## 생성된 토픽 상세조회
bin/kafka-topics.sh --bootstrap-server broker01:9092 --describe --topic hello.kafka.2
```
- 토픽 상세조회시 leader가 0으로 조회되는데 모두 0번브로커에 위치되어 있음을 알 수 있다.
- TIP 카프카 성능이 생각보다 좋지 않을경우 토픽이 브로커에 쏠려있을 경우 가 있다. 한번 확인해보는것도 좋은 방법이다.

### !warn 토픽생성시 --zookeeper가 아니라 --bootstrap-server 옵션을 사용하는 이유
- 이미 카프카를 사용해봤으면 zookeeper를 이용해 토픽을 생성해 봤을 수 도있다. 카프카 2.1 이하버전에서는 그렇게 했다. 그러나 2.2버전부터는. 카프카를 통해 한다 주키퍼와의 연관성을 줄이기위해서 이다.
- 카프카와 직접 통신하므로 --bootstrap-server 옵션을 사용해야한다.
- 

## 토픽옵션 수정
- 설정된 옵션을 변경하기 위해서는 kafka-topics.sh 또는 kafka-configs.sh 두 개를 사용해야 한다.
  - 파티션 개수 변경시에는 kafka-topics.sh 사용
  - 토픽삭제 정책인 리텐션기간 변경시 kafka-configs.sh를 사용 이런식으로 파편화된 이유는 일부로직이 다른명령어로 넘어갔기때문

- 파티션 개수를 3개에서 4개로 늘리고 리텐션기간은 172800000 에서 86400000 (1일) 로변경해보자
```bash
# 카프카 토픽 파티션 4개로 수정
bin/kafka-topics.sh --bootstrap-server broker01:9092 --topic hello.kafka --alter --partitions 4
# 확인
bin/kafka-topics.sh --bootstrap-server broker01:9092 --topic hello.kafka --describe
# 토픽 리텐션 수정 및 확인
bin/kafka-configs.sh --bootstrap-server broker01:9092 --entity-type topics --entity-name hello.kafka --alter --add-config retention.ms=86400000
bin/kafka-configs.sh --bootstrap-server broker01:9092 --entity-type topics --entity-name hello.kafka --describe
```

## 2.2.2 kafka-console-producer.sh
- hello.kafka 토픽에 위 명령어로 데이터를 넣을 수 있다.
```bash
bin/kafka-console-producer.sh --bootstrap-server broker01:9092 --topic hello.kafka
```
- 전송되는 레코드값은 utf-8 기반으로 byte로 변환되고 ByteArraySerializer로만 직렬화 된다는 점이다. 즉 String이 아닌 타입으로는 직렬화 하여 전송할 수 없다.

- 메시지 키를 가지는 데이터를 전송해보자 몇가지 추가옵션이 필요하다. (separator를 넣지않고 전송시 엑셉션을 던진다)
```bash
bin/kafka-console-producer.sh --bootstrap-server broker01:9092 --topic hello.kafka --property "parse.key=true" --property "key.separator=:"
key1:no1
key2:no2
```

## 2.2.3 kfaka-console-consumer.sh
- hello.kafka 토픽으로 전송한 데이터는 kafka-console.consumer.sh 명령어로 확인할 수 있다. 클러스터정보 및 토픽이름이 필요하다. --from-begining 옵션을 주면 가장 처음 데이터부터 출력한다.
- property를 줌으로써 key값과 함께 출력 할 수 있다.

```bash
bin/kafka-console-consumer.sh --bootstrap-server broker01:9092 --topic hello.kafka --from-beginning
bin/kafka-console-consumer.sh --bootstrap-server broker01:9092 --topic hello.kafka --property print.key=true --property=key.separator="-" --group hello-group --from-beginning
```
- --group 옵션을 통해 컨슈머 그룹을 생성했다. 컨슈머 그룹은1개이상의 컨슈머로 이루어져 있다. 이 컨슈머 그룹을 통해 가져간 토픽의 메시지는 commit을 한다
	- 커밋이란 컨슈머가 특정레코드까지 처리를 완료 했다고 레코드의 오프셋 번호를 카프카 브로커에 저장하는 것이다.
	- 이정보는 __consumer_offsets 이름의 내부토픽에 저장된다.
	- 여기서 핵심은 producer로 넣었을때랑 출력순서와 다르다는 것이다.
  - 카프카의 핵심인 파티션 개념때문에 생기는 것이다. 토픽에 넣은 순서를 보장하는것은 파티션을 1개로 구성하는 것이다.


## 2.2.4 kafka-consumer-groups.sh
- hello-group 이라는 이름의 그룹으로 hello.kafka 토픽의 데이터를 가져갔다.
- 컨슈머 그룹은 따로 생성하는 명령하는 명령을 날리지않고 컨슈머를 동작할 때 컨슈머 그룹이름을 지정하면 새로 생성된다., kafka-consumer-groups.sh 명령어로 확인가능하다.
```bash
bin/kafka-consumer-groups.sh --bootstrap-server broker01:9092 --list
# 컨슈머 그룹 내부상세조회
bin/kafka-consumer-groups.sh --bootstrap-server broker01:9092 --group hello-group --describe
```

- CURRENT-OFFSET: 컨슈머 그룹에서 마지막으로 커밋한 데이터 (읽어온 데이터)
- LOG-END-OFFSET: 파티션의 가장최신 오프셋 (데이터 개수)
- LAG: 컨슈머의 지연지술


## 2.2.5 bin/kafka-verifiable-producer, consumer.sh
- 이두개의 스크립트를 사용하면 클러스터 설치후 간단하게 데이터를 주고받아 볼 수있다. 테스트용도로 사용된다
```bash
bin/kafka-verifiable-producer.sh --bootstrap-server broker01:9092 --max-messages 10 --topic verify-test
bin/kafka-verifiable-consumer.sh --bootstrap-server broker01:9092 --topic verify-test --group-id test-group

```

## 2.2.6 kafka-delete-records.sh
- 이미 적재된 토픽의 데이터를 지우는 방법으로 kafka-delete-records.sh를 사용할 수 있다.
- 가장오래된 데이터부터 특정시점 오프셋까지 지울 수 있다.
```bash
vi delete-topic.json
{"partitions":[{"topic":"verify-test","partition":0,"offset":50}],"version":1}
bin/kafka-delete-records.sh --bootstrap-server broker01:9092 --offset-json-file delete-topic.json
# 확인
bin/kafka-consumer-groups.sh --bootstrap-server broker01:9092 --group test-group --describe
```


# 3 카프카 기본개념 설명

## 3.1 카프카 브로커, 클러스터, 주키퍼
- 카프카 브로커는 데이터를 주고받기위해 사용되는 주체이자, 데이터를 분산저장하여 장애가 발생하더라도 가용성을 높이는 애플리케이션이다.
- 하나의 서버에는 한개의 카프카 브로커 프로세스가 실행된다.
- 한개의 브로커로도 물론 가능하지만 최소 3개의 브로커를써서 1개의 클러스터로 묶어서 운영한다.
- 주키퍼는 프로듀서가 보낸 데이터를 안전하게 저장,분산하는 역할을 한다.

## 데이터 저장, 전송
- 실습용으로 진행한 카프카에서 저장된 파일시스템을 직접 확인할 수 있다.
```bash
ls /tmp/kafka-logs
ls /tmp/kafka-logs/hello.kafka.0
```

- timestamp 값은 브로커가 적재한 데이터를 삭제하거나 압축하는데 사용한다.
- 카프카는 메모리나 데이터베이스에 저장하지 않으며 따로 캐시메모리를 구현하여 사용하지도 않는다.
- 파일시스템에 저장하기 때문에 속도문제는 없을까 생각할수도 있지만 카프카는 페이지캐시를 사용하여 디스크입출력속도를 높여 이를 해결했다.
	-	페이지캐시란 OS에서 파일입출력의 성능향상을 위해 만들어놓은 메모리영역이다.
	1. 한번 읽은 파일의 내용은 메모리 페이지캐시영역에 저장시킨다.
	2. 추후 동일한 파일의 접근이 일어나면 디스크에서 읽지 않고 메모리에서 직접읽는 방식이다.
	3. 페이지 캐시를 사용하지않는다면 JVM위에서 돌아가는 카프카에서 캐시를 직접 구현해야 했을것이고, 지속적으로 변경되는 데이터때문에 gc가 자주일어 났을것이다. 그러나 페이지캐시로 이를 해결했다.
	4. 이러한 특징 때문에 카프카 브로커를 실행하는데 힙메모리 사이즈를 크게 설정할 필요가없다.

## 데이터 복제, 싱크
- 데이터복제(replication)는 카프카를 장애 허용 시스템(fault tolerant system)으로 동작하도록 하는 원동력이다.
- 카프카의 데이터복제는 파티션 단위로 이루어진다. 
- 토픽을 생성할때 replication 을 설정할 수 있으나 설정하지않으면 브로커의 설정값이다. 복제갯수의 최솟값은 1(복제없음) 이다. 최대값은 브로커의 갯수만큼 설정가능하다.


## Zookeeper
- 브로커가 컨트롤러,코디네이터, 데이터삭제, 오프셋데이터 저장등을 하는데 주키퍼는 무엇을 할까?
- 주키퍼에는 메타데이터를 관리한다
- TIP 주키퍼에서 카프카 클러스터를 사용하는방법
  - 주키퍼의 서로다른 znode에 카프카 킄ㄹ러스터를 설정하면 된다.
  - 주키퍼의 znode는 파일시스템처럼 znode 하위의 znode를 구성할 수 있다.

```bash
# 주키퍼 쉘을통해 znode를 조회하고 수정할 수있다
bin/zookeeper-shell.sh broker01:2181 
# root znode 밑에 하위 znode들을 확인한다
ls/
# 카프카 브로커의 정보를 확인한다
get /brokers/ids/0
# 어느 브로커가 컨트롤러인지 확인한다
get /controller
# 카프카에 저장된 토픽들을 확인한다.
ls /brokers/topics
```


## 3.2 토픽과 파티션
- 토픽은 데이터를 구분할때 사용하는 단위, 토픽은 1개이상의 파티션 가짐
- 파티션에는 프로듀서가 보낸 데이터들이 저장되는데 이를 record 라고 부른다.
- 파티션은 병렬처리의 핵심그룹으로써, 그룹으로 묶인 컨슈머들이 레코드를 병렬로 처리할 수 있도록 매칭된다.
- 컨슈머의 처리량이 한정된 상황에서 많은 레코드를 병렬로 처리하는 가장 좋은 방법은 컨슈머의 개수를 늘려 스케일 아웃 하는 것이다.
- 컨슈머의 개수와 파티션을 같이늘리면 처리량이 증가하는 효과를 볼 수 있다.

## 3.3 레코드
- 레코드는 타임스탬프, 메시지키, 메시지 값, 오프셋으로 구성됨
- 브로커에 한번 적재된 레코드는 수정할 수 없고, 로그 리텐션 기간 또는 용량에 따라서만 삭제된ㄷ.

## 3.4 카프카 클라이언트
- 자바 그래들로 프로듀서 프로젝트를 만든뒤 토픽을 cli로 생성해준다.
```bash
bin/kafka-topics.sh --bootstrap-server broker01:9092 --create --topic test --partitions 3
```
- 이제 프로젝트로 돌아가 ctrl+shift+R로 프로젝트를실행시켜보자
- 프로듀서를 실행시킨뒤 토픽에 레코드를 확인해보자 --from-begining 옵션을 추가로 넣어서 확인하면 모든 레코드를 확인할 수 있다.
```bash
# 토픽의 모든 레코드 확인
bin/kafka-console-consumer.sh --bootstrap-server broker01:9092 --topic test --from-beginning
# 키값또한 출력
bin/kafka-console-consumer.sh --bootstrap-server broker01:9092 --topic test --from-beginning --property print.key=true --property key.separator="-"
```

## 3.5 kafka streams

### 조인 Ktable Kstream
- KTable과 KStream 을 조인할때 가장 중요한것은 코파티셔닝이 되어있는지 여부이다
- 코파티셔닝이 되어있지않으면 TopologyException을 발생시키기 때문이다.
- 즉 동일한 파티션 개수, 동일한 파티셔닝을 사용하는것이 중요하다.
- 기본 파티셔닝 전략을 사용하도록하고 둘다 파티션을 3개로 동일하게 만들어보자
- 우리가 만들 토픽은 ardress와 order이며 adress는 ktable로 사용예정이다.
```bash
bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic order
bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic address
bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic order_join
bin/kafka-topics.sh --create --bootstrap-server broker01:9092 --topic address --partitions 3
bin/kafka-topics.sh --create --bootstrap-server broker01:9092 --topic order --partitions 3
bin/kafka-topics.sh --create --bootstrap-server broker01:9092 --topic order_join --partitions 3

# 토픽확인
bin/kafka-topics.sh --bootstrap-server broker01:9092 --list
```

- 다음과같이 스트림즈 어플리케이션을 작성후 데이터를 producer 해보자
```bash
bin/kafka-console-producer.sh --bootstrap-server broker01:9092 --topic address --property "parse.key=true" --property "key.separator=:" 
>han:Seoul
>choi:seohyun
>kim:NewYork

#주문 
bin/kafka-console-producer.sh --bootstrap-server broker01:9092 --topic order --property "parse.key=true" --property "key.separator=:" 
>han:iphone
>choi:galaxy
>kim:ipad
>An:none

#확인
bin/kafka-console-consumer.sh --bootstrap-server broker01:9092 --topic order_join --from-beginning --property print.key=true --property key.separator=":"

#바꾸고 확인
>han:jeju
```

### GlobalKTable과 Kstream을 join
```bash
bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic address_v2
bin/kafka-topics.sh --create --bootstrap-server broker01:9092 --topic address_v2 --partitions 2

bin/kafka-console-producer.sh --bootstrap-server broker01:9092 --topic address_v2 --property "parse.key=true" --property "key.separator=:"
>han:NewYork
```

- 언뜻 결과물을 보면 Ktable과 GlobalKTable의 차이가 안 보일수도 있지만, GlobalKTable로 선언한 토픽은 토픽에 존재하는 모든 데이터를 태스크마다 저장하고 조인처리를 수행한다는점이다르다.
- 또한 GlobalKatble은 메세지 키뿐만아니라 키값으로도 조인이가능하다


## 3.5.2 프로세서 API
- 프로세서 API는 스트림즈 DSL보다 투박한 코드를 가지지만 토폴로지를 기준으로 데이터를 처리한다는관점에서 동일한 역할을한다.
- 스트림즈DSL은 데이터 처리, 분기, 조인을 위한 다양한 메서드를 제공하지만, 추가적인 상세 로직이필요하다면 프로세서 API를 사용할 수 있다.
- KStream, KTable GlobalKTable 개념이 없다는 것을 주의해야한다.

```bash
bin/kafka-console-producer.sh --bootstrap-server broker01:9092 --topic stream_log
bin/kafka-console-consumer.sh --bootstrap-server broker01:9092 --topic stream_log_copy --from-beginning
```


## 3.6.1 카프카 커넥터
```bash
touch ./test.txt
# config 
bin/connect-standalone.sh config/connect-standalone.properties config/connect-file-source.properties
curl -X DELETE http://localhost:8083/connectors/local-file-source
curl -X GET http://localhost:8083/connectors/
```



-------------------------------


HAN SANG HA[ 한상하 ]님이 작성, 7월 02, 2021에 최종 변경메타 데이터의 시작으로 이동
kafka의 탄생 배경

아마 가장 일반적인 통신 아키텍쳐는 위 그림과 같이 Server-Client 모델 일 것이다.



그러나 시스템이 성장함에 따라 다양한 데이터의 흐름이 필요할 경우 복잡도가 높아져 유지보수에 있어서 굉장히 힘들어 진다.


위 그림과 같이 카프카는 파편화된 데이터 파이프라인의 복잡도를 낮추고, 데이터 흐름을 개선하기위해 링크드인의 데이터 팀에서 개발되었다.


kafka의 기본 개념
앞에 나온 kafka diagram을 좀 더 자세히 살펴보자

(1) 에서 Producer는 누가 데이터를 읽는 지 신경쓰지 않고 데이터를 topic으로 보낸다.
(2) 의 토픽이란 카프카 클러스터에서 데이터를 실질적으로 저장한다. 데이터베이스의 tables 와 유사한 목적으로 사용된다.
(3) Consumer는 하나 이상의 토픽에서 데이터를 읽거나 구독하는 프로세스이다. 직접 producer와 통신하지 않는 것이 주요한 개념이다. 오직 관심사는 Topic이다.
(4) 에서와 같이 Consumer는 Group으로 묶여 함께 작업할 수 있다. (Consumer Group)




카프카의 특징
위 그림에서 토픽은 아래처럼 여러개의 파티션으로 나눠 질 수 있다.


consumer 생성후 파티션에서 레코드를 읽어가면 읽은 부분까지 offset 을 kafka 서버에 기록(commit)한다.



또한 카프카 broker(실질적 app이 깔린 서버) 여러대를 구성하여 카프카 클러스터를 구성 할 수 있다.
카프카 클러스터를 구성하고, replication 설정을 통해 고가용성을 위한 설계를 할 수도 있다. 


(Broker 3대를 이용하여 클러스터 구성 후 3개의 파티션에 replication factor 2를 지정하였을 경우 브로커별 데이터의 저장모습)



이외에도 영속성 등의 특징을 갖는다.
용어 정리
Broker : 카프카 애플리케이션 서버 단위
Topic : 데이터 분리 단위. 다수 파티션 보유
Partition : 레코드 저장소. 컨슈머 요청시 레코드 전달
Record : 프로듀서가 생성하는 데이터. 타임스탬프, 메세지키, 메시지값, 오프셋으로 구성됨.
Offset : 각 레코드당 파티션에 할당된 레코드 고유 번호
Consumer : 레코드를 가져가는(polling) 애플리케이션
Consumer group : 다수 컨슈머 묶음
Consumer offset : 특정 컨슈머가 가져간 레코드의 번호
Producer : 레코드를 브로커로 전송하는 레코드 저장 애플리케이션
Replication : 여러개의 브로커를 구성하고, 레플리케이션을 설정하면 브로커별로 파티션을 나누어 복제본을 저장한다. (고가용성)



-------------------
# Mastering Kafka Streams 

### 도마뱀은 팁이다
### 새는 일반적인 참고사항
### 전갈은 주의사항


## Ch1 A Rapid Introduce to Kafka
---
- 가장 중요한것은 카프카의 PUB-SUB 모델은 양방향이 아니란 것이다. 즉 스트림은 한방향으로만 흐른다.
- 시스템이 일부 데이터를 Kafka 토픽으로 생성하고 다른 시스템에 의존하여 데이터로 무언가를 수행하는 경우, enriched된 데이터는 다른토픽에 씌워져야한다.

- commit Log -> append only 삭제의 개념은 없음, 각 record는 immutable 하다

### How Are Streams Stored?
- 링크드인 개발자들은 이런 물음에 빠졌다 - 묵여있지않고 지속적으로 계속되는 데이터는 어느 데이터 레이어에 모델링되어야할까?
- 데이터베이스, 키벨류스토어 vcs등등 다양하게 생각해보았지만, 커밋로그는 매우 파워풀하고 간단한 추상화였다.
- 커밋로그는 어플리케이션 로그가 아니라 실행중인 프로세스에 대한 정보를 내보냅니다.
- 유저 구매목록이란 로그를 만들어보고 아래에서 커맨드를 통해 추출해보자

- user 1이 구매를하면, 첫번째 커밋로그를 업데이트 하는것이아닌, 계속헤서 append gksms gudtlrdlqlsek.

### Topics and Partitions p9
- 따라서 로그를 배포하고 처리하는 방식으로 일정 수준의 병렬 처리를 달성하려면 로그를 많이 만들어야합니다. 이것이 Kafka 주제가 파티션이라고하는 더 작은 단위로 분리되는 이유입니다.
- 주어진 주제에 대한 파티션 수는 구성 가능하며, 한 주제에 더 많은 파티션이 있으면 일반적으로 더 많은 병렬 처리 및 처리량으로 변환됩니다.
- 토픽 파티션에 정확히 무엇이 저장될까요? 다음 주제로아랑봅시다.

### Events p11
- 물론 카프카 토픽 처리에 대한 얘기를 많이하지만, 토픽내부에 어떤 데이터가 저장되는지 이해하는것도 중요하다.
- 많은 문서에서 Evnets, 메세지, 레코드라고 다양하게 묘사한다. 책에서는 이벤트라 표현할것이다.
- 이벤트는 타임스탬프가 포함된 키밸류 기록이다.
- 키는 선태사항이지만, 파티션에 분산되는 방시겡 중요한 역할을 한다.
- value에는 바이트 배열로 인코딩된 실제내용이 포함됨.

### Kafka Cluster and Brokers p12


### Hello, kafka p16
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

- 몇가지 데이터를 생산해보자

```bash
kafka-console-producer \
 --bootstrap-server broker01:9092 \
 --property key.separator=, \
 --property parse.key=true \
 --topic users

```

- 컨슈머를 통해 가져와보자
```bash
kafka-console-consumer \
 --bootstrap-server localhost:9092 \
 --topic users \
 --from-beginning
```

-기본적으로 카프카 컨슈머는 키와 벨류만 표시함, 여러가지 프로퍼티와 같이 보내보자
```bash
./kafka-console-consumer.sh \
 --bootstrap-server broker01:9092 \
 --topic users \
 --property print.timestamp=true \
 --property print.key=true \
 --property print.value=true \
 --from-beginning
```

- Summary
 - 카프카 커뮤니케이션 모델은 여러시스템의 커뮤니케이션을 빠르고 지속가능하고, 스토리지레이어에 의존적으로 쉽게 만들어준다.
 - 클러스터를 구성함으로써, 고가용성 내결함성을 구성할 수 있다.



## Ch2 Getting Started With Kafka Streams
- 더 이상 고민하지 않고, 다음과 같은 은유적 질문으로 챕터를 시작하겠다. 카프카 스트림 어디에 살고있니?
- 1장에서 말한 핵심은 추가되는 커밋로그 이다.
- 즉, 핵심 Kafka 코드베이스에는이 로그와 상호 작용하기위한 몇 가지 중요한 API가 포함되어 있다 (이것은 메세지와 카테고리로 분류된 토픽).
  - Producer API, 메시지를 카프카 토픽에씀
  - Consumer API, 메세지를 토픽으로부터 읽음
  - Connect API , 외부 데이터 저장소, API, 파일시스템 등과 카프카 토픽을 연동함.


### Before Kafka Stream p24
- 카프카 스트림이 존재하기전에는, 생태계에는 공백이 있엇다.
- 이 공백은 스트림처리를 어렵게 만드는 공백이다.
- 기존의 API는 다음과 같은 기본요소가 부족했다
  - 내결함성 상태, 스트림 변환 연산자, 스트림의 표현, 정교한 시간처리
- 또한 아파치 플링크 스파크 와같은 다른시스템을 사용하는 것이다.

### Enter the kafka Streams p25
- Producer, Consumer 및 Connect API와 달리 Kafka Streams는 실시간 데이터 스트림을 처리하는 데 전념합니다.
- 데이터를 이동하는 것 뿐만아니라, 데이터 파이프 라인을 통해 이동할 때 이벤트의 실시간 스트림을 쉽게 소비하고 데이터를 정제한다.
- 카프카 스트림은 생태계의 "두뇌"로서 이벤트 스트림의 레코드를 소비하고 데이터를 처리하며 선택적으로 보강되거나 변환 된 레코드를 kafka에 다시 기록합니다.
- Kafka Streams는 Kafka 생태계의 흥미로운 계층, 즉 여러 소스의 데이터가 Covarage되는 곳에서 작동합니다.

### Features at a Glance p27
- Kafka Streams는 최신 스트림 처리 응용 프로그램에 탁월한 선택이되는 많은 기능을 제공합니다. 
  - Java의 스트리밍 API처럼 보이고 느껴지는 고급 DSL
  - 개발자가 필요할 때 세밀한 제어를 제공하는 저수준 프로세서 API
  - 데이터를 스트림 또는 테이블로 모델링하기위한 편리한 추상
  - 데이터 변환 및 보강에 유용한 스트림과 테이블을 조인하는 기능
  - 확장 성, 안정성, 유지 관리 용이성

### Operational Characteristics p28
- 마틴 클램프맨의 훌륭한책, 디자인 데이터 인텐시브 어플리케이션 책에서는 다음 세가지 목표를 데이터시스템에서 제시한다.
  - Scalability
    - Kafka Streams의 작업 단위는 단일 토픽 파티션이고 더 많은 파티션을 추가하여 토픽을 확장 할 수 있으므로 Kafka Streams 애플리케이션이 수행 할 수있는 작업량은 소스 토픽의 파티션 수를 늘림으로써 확장 될 수 있습니다.
    - Consumer 그룹을 활용하면 Kafka Streams 애플리케이션이 처리하는 총 작업량을 애플리케이션의 여러 협력 인스턴스에 분산시킬 수 있습니다.
  - Reliability
    - "Consumer 그룹"에서 이미 다루었던 것입니다. 소비자 그룹을 통한 자동 장애 조치 및 파티션 재조정입니다. p13 (소비자 그룹을 통한 자동 장애 조치 및 파티션 재조정.)
  - Maintainability
    - Kafka Streams는 Java 라이브러리이므로 독립 실행 형 애플리케이션으로 작업하고 있기 때문에 버그 문제 해결 및 수정이 비교적 간단하며 생태계를 통한 모니터링도 쉽다.

### 타 시스템과 비교 p30
 - Kafka Streams는 한번에 이벤트 처리를 구현하므로 마이크로 배치보다 지연시간이 짧다

- Kappa Architecture (Kafka Stream)
- 두 시스템 간의 차이점을 설명하는 한 가지 방법은 다음과 같습니다. 
  - Kafka Streams는 스트림 관계형 처리 플랫폼입니다. 
  - Apache Beam은 스트림 전용 처리 플랫폼입니다.


### Processor Topologies p35
- Kafka Streams는 프로그램을 일련의 입력, 출력 및 처리 단계로 표현하는 데이터 중심 방법 인 데이터 흐름 프로그래밍 (DFP)이라는 프로그래밍 패러다임을 활용합니다.
- 이 장은 깊게설명하므로 45p 부터 읽고 다시방문해도 좋습니다.
- 토폴로지란 프로그램을 일련의 단계로 빌드하는 대신 Kafka Streams 애플리케이션의 스트림 처리 논리는 방향성 비순환 그래프(DAG)로 구성됩니다.

### Introducing Our Tutorial: Hello, Streams p45
- DSL 이란 DSL(Domain Specific Language) 이다. kafka Streams DSL을 제공
- Kafka Streams 에서 토폴로지는 노드와 '노드와 노드를 이은 선'으로 이루어져 있는데, 노드는 프로세서(Processor), 선은 스트림(Stream)을 의미합니다. 즉, 데이터를 처리하는 녀석이 노드이고 다음 노드로 넘어가는 데이터를 선이라고 보시면 될 것 같습니다.
- 토폴로지란 데이터의 흐름을 표현하기 위해 노드와 선으로 이루어진 도식화.

- 참고 스트림즈의 구성요소
  - 소스 프로세서 (Source Processor)
	소스 프로세서는 토폴로지의 시작 노드이며,
	데이터를 처리하기 위해 최초로 선언해야 하는 노드입니다.
	카프카와 연결된 프로세서이며, 하나 이상의 토픽에서 데이터를 가져오는 역할을 합니다.

	- 스트림 프로세서 (Stream Processor)
 	스트림 프로세서는 다른 프로세서(소스 프로세서, 스트림 프로세서)가 반환한 데이터를 처리하는 역할을 합니다.

	- 싱크 프로세서 (Sink Processor)
	싱크 프로세서는 토폴로지의 마지막 노드이며,
	데이터 전달을 위해 마지막에 선언해야 하는 노드입니다.
	카프카와 연결된 프로세서이며, 데이터를 카프카의 특정 토픽으로 저장하는 역할을 합니다.


- 카프카 스트림즈를 구현하는 2가지 방법
 	1. 스트림즈 DSL
  - 스트림즈 DSL에서는 데이터 흐름을 추상화한 3가지 개념이 있다. KStream, Ktable, GlobalKtable이다.
  - Kstream은 데이터가 AppendLog 뒤에 추가된것도 모두 출력된다. Key의 중복을 허용
  - Ktable에서는 Key가 동일한경우에는 최신데이터로 value를 덮어씌운다.
  2. 프로세서 API
  - 앞서 사용했던 추상화 개념인 Kstream, Ktable, Global KTable 모두 사용하지 않는다.

  - 결론: 스트림즈를 구현하는 2가지 프로세스는 결국 어떻게 구현을 하던지 간에 Source > Stream > Sink의 처리


- 참고 : https://velog.io/@jwpark06/Kafka-%EC%8A%A4%ED%8A%B8%EB%A6%BC%EC%A6%88-%EC%95%8C%EC%95%84%EB%B3%B4%EA%B8%B0




### Streams And Table p57
- 스트림과 Table은 자바로 비교했을때 List 와 Map으로 비교하면 이해하기 쉽다. 리스트는 시계열 데이터를 저장할 수 있지만, 그 리스트의 요소를 맵으로 저장하면 키값에 따라 업데이트를 시킴

### Kstream, Ktable, GlobalKatable 
- KStream KStream은 데이터가 삽입 의미론을 사용하여 표현되는 분할된 레코드 스트림의 추상화입니다(즉, 각 이벤트는 다른 이벤트와 독립적인 것으로 간주됨)
- KTable은 파티셔닝된 테이블의 추상화 이다.
- GlobalKTable 이것은 각 GlobalKTable에 기본 데이터의 완전한(즉, 분할되지 않은) 사본이 포함되어 있다는 점을 제외하고는 KTable과 유사합니다. 


0. 우리쪽 키는 상품 아이디로 사용한다.
1. 궁금한점 토폴로지 설명에서처럼 소스 프로세서 -> 스트림 프로세서 -> 싱크 프로세서 이 3개는 무조건 있어야하는 필수요소 일까? (가끔 궁금할때 라인쇼핑 아키텍쳐를 보는데 3요소가 이루어진것같지않다.)
2. processor 하나가 어플리케이션 하나가 아니다.

----

## CH3 Stateless Processing
- 단순한 레코드처리에는 이전 이벤트나 메모리를 요구하지 않는다.
- 다음과 같은 기본처리를 할 수 있다.
	- 레코드 필터링 
	- 필드 추가 및 제거 
	- 레코드 다시 입력 
	- 스트림 분기 
	- 스트림 병합 
	- 레코드를 하나 이상의 출력으로 변환 
	- 한 번에 하나씩 레코드 강화


### Stateless Versus Stateful Processing
 - 위 둘을 비교하기전에, 카프카 스트림 어플리케이션의 상태저장이 필요한지 아닌지 고려해야한다
 - 비교
 	1. 상태 비저장 애플리케이션에서 Kafka Streams 애플리케이션이 처리하는 각 이벤트는 다른 이벤트와 독립적으로 처리되며 애플리케이션에서는 스트림 보기만 필요합니다.
 	2. 반면에 상태 저장 애플리케이션은 프로세서 토폴로지의 하나 이상의 단계에서 이전에 본 이벤트에 대한 정보를 기억해야 합니다.

- StateLess 연산자 - filter, map, flatMap, join etc
- Stateful 연산자 - counte emd


### Introducing Our Tutorial: Processing a Twitter Stream
- 듀토리얼 설명
	1. 암호화폐에대한 언급이 있는 트윗은 tweets 이라는 토픽으로부터 컨슘될것이다.
	2. 리트윗은 처리에서 제외되어야 합니다. 여기에는 일종의 데이터 필터링이 포함됩니다.
	3. 영어로 작성되지 않은 트윗은 번역을 위해 별도의 스트림으로 분기해야 합니다.
	4. 영어가 아닌 트윗은 영어로 번역해야 합니다. 여기에는 하나의 입력 값(영어가 아닌 트윗)을 새 출력 값(영어로 번역된 트윗)에 매핑하는 작업이 포함됩니다.
	5. 새로 번역된 트윗을 영어 트윗 스트림과 병합하여 하나의 통합 스트림을 만들어야 합니다.
	6. 각 트윗에는 트위터 사용자가 특정 디지털 통화에 대해 감정 점수가 포함되어야 합니다. 단일 트윗에 여러 암호화폐가 언급될 수 있으므로 flatMap 연산자를 사용하여 각 입력(트윗)을 가변 수의 출력으로 변환하는 방법을 보여줍니다.
	7. 정제된 트윗은 crypto-sentiment라는 topic에 저장.

### Adding a KStream Source Processor
- Kstream 은 인터페이스이다. byte[] 를 k,v 인자로 줄경우
	- 트윗 주제에서 나오는 레코드 키와 값이 바이트 배열로 인코딩됨을 나타냅니다.



### Building a Custom Serdes 



### Defining Data Classes
- 사용하지 않는 필드를 Pojo 데이터에 추가하지 않는 것을 projection 이라고하며 Select 문과 유사하다.


### Tweet pojo Serializer, DeSerializer with Gson

### Building the Tweet Serdes
- Kafka Streams가 deserializer와 serializer를 사용하기위해 Wraaping 하는것이 Serdes 클래스이다


### Filtering Data


### Branching Data


### Enriching Tweets p82
- Tweet VO는 Topic의 구조를 나타냅니다. crypto-sentiment에 저장할 데이터 형식을 만들어봅시다.
- 우리는 Avro라는 데이터 직렬화 형식을 사용할 것입니다.

### Avro Data Class p83
- Avro는 카프카 커뮤니티에서 가장 인기있고, 강력한 포맷입니다.
- generic records or specific records 형태를 사용 할 수 있습니다.
	1. generic records는 런타임에 레코드 스키마를 알 수 없는 경우에 적합합니다. 일반 getter 및 setter를 사용하여 필드 이름에 액세스할 수 있습니다.
	2. specific records는 Avro 스키마 파일에서 생성된 Java 클래스입니다

### Sentiment Analysis
- flatMap 및 flatMapValues는 모두 호출될 때마다 0, 1 또는 여러 출력 레코드를 생성할 수 있습니다
- 

### Serializing Avro Data
- Avro 스키마를 사용하여 serialize를 하는데에는 두가지 선택이있다.
	1. 각 레코드에 Avro 스키마를 포함합니다. 
	2. Avro 스키마를 Confluent Schema Registry에 저장하고 전체 스키마 대신 각 레코드에 훨씬 더 작은 스키마 ID만 포함하여 훨씬 더 간결한 형식을 사용합니다.


### Adding a Sink Processor p90
- 마지막단계는 enrich data를 crypto-sementic topic에 쓰는것이다. 이를수행하는데는 몇가지 연산자가있다.
	1. to
	2. through
	3. repatition
- 새 KStream을 반환하려면 repartition or thorugh를 호출하면 된다.
- 스트림에서 터미널 단계에 도달했다면 기본 KStream에 다른 스트림 프로세서를 추가할 필요가 없기 때문에 void를 반환하는 to 연산자를 사용해야 합니다.





## CH4 Stateful Processing

### Benefits of Stateful Processing 96p

### preview of stateful operators
	1. join
	- 별도의 스트림 또는 테이블에 캡처된 추가 정보 또는 컨텍스트로 이벤트를 강화합니다.
	2. aggregating data 
	- 관련 이벤트의 지속적으로 업데이트되는 수학적 또는 조합 변환을 계산합니다.
	3. windowing data
	- 시간적으로 가까운 그룹 이벤트(파티셔닝 테이블을 생각해보자)

### state stores
- 상태저장 작업을 지원하려면, 필요한 기억된 데이터 또는 상태를 저장하고 검색하는 방법이 필요합니다.
- Kafka Streams에서 이러한 요구 사항을 해결하는 저장소 추상화를 State Store 라고 함


### Common Chracteristics
- Kafka Streams에 포함된 기본 상태 저장소 구현은 몇 가지 공통 속성을 공유합니다.
1. Embedded
	- 네트워크 호출이 불필요하므로 불필요한 대기 시간과 처리 병목 현상이 발생안함.
	- 모든 state store는 기본적으로 RocksDB 를 사용한다. facebook에서 개발된 빠르내장형 키값저장소
2. Multiple access modes
	- 상태 저장소는 여러 액세스 모드와 쿼리 패턴을 지원합니다.
3. Fault tolerant
	- 오류가 발생한 경우 기본 changelog topic에서 개별 이벤트를 재생하여 응용 프로그램 상태를 재구성하여 상태 저장소를 복원할 수 있습니다.
4. key-based
	- 상태 저장소를 활용하는 작업은 키 기반입니다. 레코드의 키는 현재 이벤트와 다른 이벤트 간의 관계를 정의합니다.

### Persistent Versus In-Memory Stores 101p
- Persistent 장점
	1. 상태가 사용 가능한 메모리 크기를 초과할 수 있습니다
	2. 장애가 발생한 경우 영구 저장소는 인메모리 저장소보다 빠르게 복원할 수 있습니다.
- 단점
	1. 운영상복잡
	2. 인메모리보다 느릴수 있음

- 인메모리 저장소의 성능향상이 생각보다 drastic 않을수 있다. (장애복구가 더오래걸리기 때문에)


### Introducing Our Tutorial: Video Game Leaderboard p102
1. 1tier
	- score-event 주제에는 게임 점수가 포함됩니다. 레코드는 키가 없으므로 주제 파티션 전체에 라운드 로빈 방식으로 배포됩니다. 
	- 선수 주제에는 선수 프로필이 포함되어 있습니다. 각 레코드는 플레이어 ID로 키가 지정됩니다. 
	- 제품 항목에는 다양한 비디오 게임에 대한 제품 정보가 포함되어 있습니다. 각 레코드는 제품 ID로 키가 지정됩니다.

2. 2tier
	- 자세한 플레이어 정보로 점수 이벤트 데이터를 보강해야 합니다. 조인을 사용하여 이를 수행할 수 있습니다.

3. 3tier
	- 플레이어 데이터로 점수 이벤트 데이터를 보강했으면 결과 스트림에 자세한 제품 정보를 추가해야 합니다. 이는 조인을 사용하여 수행할 수도 있습니다.

4. 4tire 
	- 데이터를 그룹화하는 것은 집계의 전제 조건이므로 강화된 스트림을 그룹화해야 합니다.

5. 5tier
	- 각 게임의 상위 3개 최고 점수를 계산해야 합니다. 이를 위해 Kafka Streams의 집계 연산자를 사용할 수 있습니다.

6. 6tier
	- 마지막으로 각 게임의 최고 점수를 외부에 공개해야 합니다. Kafka Streams의 대화형 쿼리 기능을 사용하여 RESTful 마이크로서비스를 구축함으로써 이를 달성할 것입니다.


### data modeling
1. ScoreEvent.java 데이터 클래스는 score-event topic 레코드를 나타내는 데 사용됩니다. 
2. Player.java 데이터 클래스는 playes topic 레코드를 나타내는 데 사용됩니다. 
3. Product.java 데이터 클래스는 products topic 레코드를 나타내는 데 사용됩니다.


### Adding the Source Processors
- 총세개의 topic을 읽을것이기때문에 3개의 source processor가 필요하다. 지금까지는 Kstream만 사용해왔다.
- 우리 토폴로지에서는 제품과 플레이어 topic을 모두 조회로 사용해야하므로 테이블과 같은 추상화가 이러한 주제에 적합할 수 있다는 좋은 표시입니다 
- Kafka Streams 추상화에 매핑하기 시작하기 전에 먼저 Kafka 주제의 KStream, KTable 및 GlobalKTable 표현 간의 차이점을 검토합니다.
	1. kstream
	- 하나 이상의 데이터 소스에 mutable(가변) 테이블 semantics(의미)가 필요하지 않을 때 KTable 또는 GlobalKTable과 함께 stateless KStream을 사용하는 것도 매우 일반적입니다.
  - 테이블은 키 기반이므로 키가 없는 score event topic에 대해 KStream을 사용해야 한다는 강력한 표시입니다. (앞서 스코어 이벤트토픽은 키없이 라운드로빈방식으로 배포된다고 설명됨)
  - 게다가, 우리 애플리케이션은 최신 점수가 아니라 각 플레이어의 최고 점수에 관심을 갖기 때문에 테이블 의미없다.

  2. Ktable
  - 플레이어 topic는 플레이어 프로필을 포함하는 압축된 주제이며 각 레코드는 플레이어 ID로 키가 지정됩니다
  - 플레이어의 최신 상태에만 관심이 있기때문에 Table기반 추상화가 좋다.
  - 키스페이스가 매우 크거나(즉, 높은 카디널리티/많은 고유 키를 가짐) 매우 큰 키스페이스로 성장할 것으로 예상되는 경우 Ktable
  - Table 또는 Global KTable 중에서 선택할 때 더 중요한 고려 사항은 시간 동기화 처리가 필요한지 여부입니다
  - 타임스탬프를 보고 다음에 처리할 레코드를 결정합니다 반면에 GlobalKTable은 시간 동기화되지 않으며 "처리가 완료되기 전에 완전히 채워집니다." .
  - [그림] 여러 애플리케이션 인스턴스에 걸쳐 상태를 분할하고 시간 동기화 처리가 필요한 경우 KTable을 사용해야 합니다.

  3. Global KTable
  - product ID에 대해 최신기록을 유지해야하므로 player topic과 유사하다.
  - 제품 주제는 플레이어 주제보다 훨씬 더 낮은 카디널리티를 가지며, 이는 인메모리에 들어갈만큼 충분히 작은 공간을 차지합니다.
  - 제품 주제의 데이터는 더 작을 뿐만 아니라 상대적으로 정적입니다.
  - [그림] co-partioning을 피하며.  및 시간 동기화가 필요하지 않은 경우에 사용해야 합니다.


### joins p111
- 따라서 단순 병합 작업은 병합되는 이벤트에 대한 추가 컨텍스트가 필요하지 않으므로 상태 비저장입니다.
- 관계형 시스템과 마찬가지로 Kafka Streams는 여러 종류의 조인을 지원합니다
1. inner join
	- 조인 내부 조인. 조인의 양쪽에 있는 입력 레코드가 동일한 키를 공유하면 조인이 트리거됩니다.
2. left join
	- 스트림 테이블 조인의 경우: 조인의 왼쪽에 있는 레코드가 수신될 때 조인이 트리거됩니다. 조인의 오른쪽에 동일한 키를 가진 레코드가 없으면 오른쪽 값이 null로 설정됩니다.
	- 스트림 스트림 및 테이블 테이블 조인의 경우: 스트림 스트림 왼쪽 조인과 의미가 동일하지만 조인의 오른쪽에 있는 입력도 조회를 트리거할 수 있다는 점을 제외하고는 다릅니다. 오른쪽이 조인을 트리거하고 왼쪽에 일치하는 키가 없으면 조인은 결과를 생성하지 않습니다.
3. outer join
	- 조인의 양쪽에 있는 레코드가 수신되면 조인이 트리거됩니다. 조인의 반대쪽에 동일한 키를 가진 일치하는 레코드가 없으면 해당 값이 null로 설정됩니다.
	참고 URL : https://m.blog.naver.com/syam2000/222158302362


### 까마귀 scoreEvents는 조인의 왼쪽입니다. 플레이어는 조인의 오른쪽입니다.


### Join Types
- 지금은 Co-partitioning 실제로 조인을 수행하는 데 필요한 추가 요구 사항 집합이라는 것을 이해하는 것으로 충분합니다.

이 장에서 수행해야 하는 두 가지 유형의 조인은 다음과 같습니다. 
- KStream-KTable은 스코어 이벤트에 조인합니다. KStream 및 플레이어 KTable 
- KStream-GlobalKTable은 이전 조인의 출력을 제품 GlobalKTable과 조인합니다.
- 첫 번째 조인(KStream-KTable)에서 공동 분할이 필요함을 알 수 있습니다.

### Co-partitioning
- stream의 selectKey를 이용하여 rekeying 작업을해서 파티션에 재분배하는것
- 이유는 Kstream을 조인을 할때 파티션이 같을때만 조인이된다. 그러므로 Co-Patitioning이 필요하다
- ##궁금점, sink processor가 따로연결되어 있지않은데 한다고해서 topic이 co-partitioning이 되는것일까?
- GlobalKtable에서 글로벌이란 전체파티션을 본다는 의미의 Global이다.


### Summary
- Kafka Streams가 consume하는 이벤트(토픽)에 대한 정보를 캡처하는 방법과 기억된 정보(state)를 활용하여 다음을 포함한 고급 스트림 처리 작업을 수행하는 방법을 배웠습니다.
	1. join Kstream - Ktable
	2. join type 타입에따라 코파티셔닝을 하기위한 Rekying message (score-events -> players)
	3. join Kstream - Global Table (withPlayyes -> withProducts)
	4. 집계를 위해 데이터를 준비하기 위해 레코드를 중간 표현(KGroupedStream, KGroupedTable)으로 그룹화 (해서 materilize에 state 저장)	(groupBy를 사용하는 것은 selectKey를 사용하여 스트림을 다시 입력하는 프로세스와 유사합니다)
	5. aggregation 스트림 과 테이블
	6. 대화형 쿼리를 사용하여 로컬 및 원격 쿼리를 모두 사용하여 애플리케이션 state 노출





# chapter 5 Windows and Time

윈도우란 일정 타임마다 record들을 누적시켜서 집합단위로



### 이번 장이 끝나면 알수 있는 것들
- 이벤트 시간, 수집 시간 및 처리 시간의 차이점 
• 이벤트를 특정 타임스탬프 및 시간 의미와 연결하기 위한 사용자 지정 타임스탬프 extractor를 구축하는 방법 
• Kafka Streams를 통한 데이터 시간흐름을 제어하는 방법 
• Kafka Streams에서 지원되는 window 유형  
• Window Join을 수행되는 방법 
• Window aggregations를 수행하는 방법 
• 지연 및 비순차 이벤트를 처리하는 데 사용할 수 있는 전략 
• widnow의 최종 결과를 처리하기 위해 suppress 연산자를 사용하는 방법 
• 키-값 저장소에 쿼리하는 방법 Windowed된 








### Introducing Our Tutorial p144
- 체온과 심박수라는 두 가지 바이탈이 모두 미리 정의된 임계값(심박수 >= 100회/분, 체온 >= 100.4°F)에 도달하면 해당 의료 직원에게 알리기 위해 alert topic에 기록을 보냅니다.

- topology
	1. 환자 ID로 키가 입력됩니다.
	2. 펄스 이벤트를 심박수로 변환해야 합니다(분당 비트 또는 bpm을 사용하여 측정)
		- aggregation을 위해 records를 그룹화
	3. 심박수 측정 위해 윈도우 집계를 사용할 것입니다. 측정 단위는 분당 비트이므로 창 크기는 60초가 됩니다.
	4. suppress 오퍼레이션을 사용합니다. 최종으로 계산된 bpm window만 emit 하기위해
	5. 감염을 감지하기 위해 사전 정의된 임계값 집합(심박수 >= 분당 100회, 체온 >= 100.4°F)을 위반하는 모든 생체 측정값을 필터링합니다
	6. window aggregations는 recode key를 change합니다. 따라서 기록을 결합하기 위한 co-patitioning 요구사항을 충족하려면 환자 ID별로 심박수 기록을 다시 입력해야 합니다.
	7. 두 개의 vitals 스트림을 결합하기 위해 window join을 수행합니다. bpm 및 체온 측정을 필터링한 후 join을 수행하기 때문에 결합된 각 레코드는 SIRS에 대한 경고 조건을 나타냅니다.(전신성 염증 반응 증후군)
  8. 인터렉티브 쿼리를 통해 결과를 외부에 제공합니다. 또한 joined 스트림의 출력을 alerts라는 topic에 씁니다.






### Data Models
- timestamp extractor를 위해 vital interface라는 것으 만들것임
- 지금까지 레코드가 타임스탬프와 연결되는 방식에 대해 많이 생각하지 않았습니다. 따라서 입력 스트림을 등록하기 전에 Kafka Streams의 다양한 시간 의미를 살펴보겠습니다.






### Time Semantics
-  Kafka Streams의 다양한 시간 개념을 이해
1. event time : 이벤트 시간 소스에서 이벤트가 생성된 시간입니다
2. Ingestion time:  시간 이벤트가 Kafka 브로커의 topic에 추가되는 시간
3. Processing time : 처리 시간 Kafka Streams 애플리케이션에서 이벤트를 처리하는 시간입니다

- 이벤트시간은 가장 직관적이며, payload에 내제된다.

- 시맨틱과 관련된 구성은 다음과 같다
- log.message.timestamp.type (broker level)
- message.timestamp.type (topic level)





### time stamp extractor
- 각 타임스탬프 추출기 구현은 다음 인터페이스를 준수해야 합니다.
- Kafka Streams에서 타임스탬프 추출기는 주어진 레코드를 타임스탬프와 연결하는 역할을 하며 이러한 타임스탬프는 윈도우 결합 및 윈도우 집계와 같은 시간 종속 작업에 사용됩니다.






### Included Timestamp Extractors
- 환자모니터링 애플리케이션같은경우 처리시간을 사용할경우 의도하지않은 부작용을 야기한다
	- ex) 1분 내에서 하트비트 수를 캡처하려고 합니다.
	- If we use processing-time semantics (e.g., via WallclockTimestampExtractor)
	- 애플리케이션에 몇 초의 지연이 발생하더라도 이벤트가 의도한 범위를 벗어나 특정 방식으로 예상에 영향을 미칠 수 있습니다(즉, 상승된 심박수를 감지하는 능력).



-까마귀 = 타임스탬프가 추출되고 이후에 레코드와 연결되면 해당 레코드를 stamped라고 합니다.



### Custom Timestamp Extractors
- VitalTimestampExtractor를 구현할 때 파티션 시간으로 대체하기로 결정했습니다. 파티션 시간은 현재 파티션에서 이미 관찰된 가장 높은 타임스탬프로 해석됩니다. 
- 이제 타임스탬프 추출기를 만들었으므로 입력 스트림을 등록해 보겠습니다.

######### epoch -> 1세대 epoch 2세대 epoch 여기서 윈도의 크기 만큼 (상대적으로 구분된 시간)



### Window Types
- There are four different types of windows in Kafka Streams. 

1. tumbling window 
- 텀블링 창은 절대 겹치지 않는 고정 크기의 창입니다. 그것들은 단일 속성인 창 크기(밀리초)를 사용하여 정의되며 epoch와 정렬되기 때문에 예측 가능한 시간 범위를 가집니다. 

2. Hopping windows
- 창 크기와 진행 간격(창이 앞으로 이동하는 정도)을 모두 지정해야 합니다. 그림 5-4와 같이 진행 간격이 창 크기보다 작으면 창이 겹치므로 일부 레코드가 여러 창에 나타날 수 있습니다. 또한 호핑 윈도우는 에포크와 정렬되기 때문에 예측 가능한 시간 범위를 가지며 시작 시간은 포함되고 종료 시간은 제외됩니다

3. Session windows
- session window는 활동 기간에 따라 결정되는 가변 크기 창입니다. inactivity gap 이라고 하는 단일 매개변수는 세션 창을 정의하는 데 사용됩니다. 세션 창은 정렬되지 않고(범위는 각 키에 따라 다름) 길이가 가변적입니다. 범위는 기록 타임스탬프에 완전히 의존하

4. sliding join windows
- 타임스탬프 간의 차이가 창 크기보다 작거나 같으면 두 레코드가 동일한 창에 속합니다. 따라서 세션 window과 유사하게 아래쪽 및 위쪽 창 경계가 모두 포함됩니다. 


- Sliding aggregation windows
- 슬라이딩 결합 창과 마찬가지로 슬라이딩 집계 창의 창 경계는 레코드 타임스탬프(에포크와 반대)에 맞춰 정렬되며 하단 및 상단 window 경계가 모두 포함됩니다. 또한 타임스탬프 간의 차이가 지정된 창 크기 내에 있는 경우 레코드는 동일한 창에 속합니다. 다음은 지속 시간이 5초이고 유예 기간이 0초인 슬라이딩 창을 만드는 방법의 예입니다





### select a window
- 세션 창은 심박수 측정에 적합하지 않습니다. 스트림에 activity가 있는 한 window 크기가 무한정 확장될 수 있기 때문입니다. 이것은 60초의 고정 크기 창을 갖는 우리의 요구 사항을 충족하지 않습니다. 또한 슬라이딩 조인 창은 조인에만 사용되므로 이 선택도 배제할 수 있습니다(이 자습서의 뒷부분에서 슬라이딩 조인 창을 사용함).

- 간단하게 유지하기 위해 창 경계를 에포크(슬라이딩 집계 창 제외)에 맞추고 창 중첩을 피합시다. tumbling window





### Window Aggreagtions
- KTable의 키가 String에서 Windowed<String> 으로 변경되었다는 것입니다. 이것은 windowedBy 연산자가 원래 레코드 키뿐만 아니라 창의 시간 범위도 포함하는 다차원 키를 갖는 창형 KTables로 KTables를 변환하기 때문입니다.






### Emitting Window Results
- window aggregation의 result를 언제 emit할지는 굉장히 어렵다. 복잡성은 다음 두가지로 인해발생한다
1. 무제한 이벤트 스트림은 특히 event-time semantics(record발생시 타임스탬프)를 사용할 때 항상 타임스탬프 순서가 아닐 수 있습니다.
2. Event are sometimes delayed
(Kafka는 이벤트가 파티션 수준에서 항상 오프셋 순서로 유지되도록 보장합니다. 즉, 모든 소비자는 항상 토픽에 추가된 것과 동일한 순서로 이벤트를 읽습니다)

- 타임스탬프 순서가 없다는 것은 특정 타임스탬프가 포함된 레코드를 찾은 경우 해당 타임스탬프 이전에 도착했어야 하는 모든 레코드를 모았고 따라서 이것을 final window result라고 생각하고 emit할 수 있다고 가정할 수 없음을 의미합니다. 
- 모든 데이터가 도착할 때까지 일정 시간을 기다릴 것인지, 아니면 업데이트될 때마다 window result를 출력할 것인지, 이것은 완전성과 지연(latency) 사이의 trade-off 이다.
- 데이터를 기다리는 것이 완전한 결과를 생성할 가능성이 높기 때문에 이 접근 방식은 완전성을 최적화합니다.
- 모든것은  service-level agreements (SLAs) 에 달려있다.



- 먼저 Kafka Streams에서 지연된 데이터를 처리하는 전략을 살펴보겠습니다.
- 그런 다음 Kafka Streams의 supress 연산자를 사용하여 중간 창 계산을 억제하는 방법을 배웁니다.)

time semantic -> 3가지를 명시, 이벤트 타임이 순서대로 안들어올 경우가있다 (레코드안에있는것) 카프카는 보장할수없다. unbounded(카프카 시간에 엮이지않는)





### Grace period

- 워터마크는 주어진 창에 대한 모든 데이터가 도착해야 하는 시기를 추정하는 데 사용됩니다(일반적으로 창 크기와 이벤트의 허용된 지연을 구성하여). 그런 다음 사용자는 지연 이벤트(워터마크에 의해 결정됨)를 처리해야 하는 방법을 지정할 수 있으며, 지연 이벤트를 삭제하는 것이 인기 있는 기본값(Dataflow, Flink 및 기타에서)입니다. 워터마크 접근 방식과 유사하게 Kafka Streams를 사용하면 유예 기간을 사용하여 허용되는 이벤트 지연을 구성할 수 있습니다. 
- 유예 기간을 더 늘릴 수는 있지만 트레이드오프를 기억하십시오. 유예 기간이 길수록 창을 더 오래 열어 두기 때문에(데이터가 지연될 수 있음) 완전성을 위해 최적화되지만 대기 시간이 길어집니다

## event time을 카프카가 보상할수없으니, 유예시간을 둔다.




### Suppression
- 이전 섹션에서 배웠듯이 새로운 데이터가 도착할 때마다 창의 결과를 내보내는 것을 포함하는 Kafka Streams의 지속적인 개선 전략은 짧은 대기 시간에 최적화하여 불완전한 result가 emit 된다
- supress 연산자는 창의 최종 계산만 내보내고 다른 모든 이벤트를 supress(즉, 메모리에 중간 계산을 일시적으로 유지)하는 데 사용할 수 있습니다.
- In order to use the suppress operator, we need to decide three things
1. 중간 창 계산을 억제하기 위해 사용해야 하는 억제 전략 
2. 억제된 이벤트를 버퍼링하는 데 사용해야 하는 메모리 양(이것은 버퍼 구성을 사용하여 설정됨) 
3. 이 메모리 제한을 초과할 때 수행할 작업(버퍼 가득 참을 사용하여 제어됨) 전략)

- surpress strategies
	1. Suppressed.untilWindowCloses : 창의 최종 결과만 내보냅니다
	2. Suppressed.untilTimeLimit : 마지막 이벤트가 수신된 후 구성 가능한 시간이 경과한 후 창의 결과를 내보냅니다

- 용량제한 스트레테지
- buffer full starategies


- 우리앱의 전략 선택 
1. 대신, 우리는 심박수 창의 최종 결과만 내보내고자 하므로 WindowClose가 여기에 더 적합할 때까지입니다. 
2. 키 공간이 상대적으로 작을 것으로 예상하므로 무제한 버퍼 구성을 선택합니다. 
3. 부정확한 심박수 계산으로 이어질 수 있으므로 결과를 일찍 내보내고 싶지 않습니다(예: 20초가 경과한 후에 하트비트 수를 내보낸다면). 따라서 우리는 Buffer Config 전략으로 shutDownWhenFull을 사용할 것입니다.






### Windowed KTable 필터링 및 키 재지정
- 예제 5-4의 KTable을 자세히 보면 윈도우로 인해 키가 String 유형에서 Windowed<String> 으로 변경되었음을 알 수 있습니다. 
-  이전에 언급했듯이 window 설정으로 인해 추가 차원인 window 범위로 레코드가 그룹화되기 때문입니다
- 따라서 body-temp-events 스트림에 참여하려면 pulse-events 스트림의 키를 다시 지정해야 합니다.
- 프로세서 토폴로지에서 5단계와 6단계를 완료했으므로 창 조인을 수행할 준비가 되었습니다.






### Windowed Joins
- Window Join을 하려면 sliding join window. 가 필요합니다
- Windowed joins are required for KStream-KStream 
- 따라서 관련 값을 빠르게 조회하려면 데이터를 로컬 상태 저장소로 구체화해야 합니다.





### Time Driven DataFlow

- stream processor application은 특히 여러 소스의 과거 데이터를 처리할 때 정확성을 보장하기 위해 !!!입력 스트림을 동기화하는 것이 중요합니다. 
- 이 동기화를 용이하게 하기 위해 Kafka Streams는 각 스트림 작업에 대해 단일 파티션 그룹을 만듭니다. 
- 파티션 그룹은 우선 순위 대기열을 사용하여 주어진 작업에서 처리되는 각 파티션에 대해 대기열에 있는 레코드를 버퍼링하고 처리할 다음 레코드(모든 입력 파티션에서)를 선택하는 알고리즘을 포함합니다. 타임스탬프가 가장 낮은 레코드가 처리를 위해 선택됩니다.

- [그림] record 타임스탬프를 비교하여 Ka€a Streams 애플리케이션을 통해 데이터가 흐르는 방식을 결정합니다.


### alert sink

- 이제 윈도우 aggregation(환자의 심박수를 계산함) 결과를 노출하여 윈도우 키-값 저장소를 쿼리하는 방법을 알아보겠습니다.


### Querying Windowed Key-Value Stores
- 이전 131페이지의 "창이 없는 키-값 저장소 쿼리"에서 창이 없는 키-값 저장소를 쿼리하는 방법을 보았습니다.
-  그러나 윈도우 키-값 저장소는 레코드 키가 다차원적이며 원래 레코드 키(윈도우가 없는 키에서 볼 수 있는 것)가 아니라 원래 키와 윈도우 범위로 구성되기 때문에 다른 쿼리 세트를 지원합니다. 키-값 저장소). 먼저 키 및 창 범위 스캔을 살펴보겠습니다.


- 키 + 윈도우 범위 스캔 윈도우 키-값 저장소에 사용할 수 있는 두 가지 유형의 범위 스캔이 있습니다. 
- 첫 번째 유형은 주어진 윈도우 범위에서 특정 키를 검색하므로 세 개의 매개변수가 필요합니다.
1. 검색할 키(환자 모니터링 애플리케이션의 경우 환자 ID, 예: 1에 해당)  
2. epoch(예: 1605171720000)에서 밀리초로 표시되는 창 범위의 하한 2020-11-12T09:02:00.00Z)
3.  창 범위의 상한 경계, 에포크로부터 밀리초로 표시(예: 1605171780000, 2020-11-12T09:03:00Z로 변환)

- 선택한 시간 범위에서 각 키를 반복하는 데 사용할 수 있는 iteration 반환합니다

-  두 번째 유형의 범위 스캔은 주어진 시간 범위 내의 모든 키를 검색합니다. 이 유형의 쿼리에는 두 개의 매개변수가 필요합니다.
from time , to time

- all entries
- 모든 항목 범위 스캔 쿼리와 유사하게 all() 쿼리는 로컬 상태 저장소에서 사용 가능한 모든 윈도우 키-값 쌍에 대한 반복자를 반환합니다
- 이러한 쿼리 유형을 사용하여 이전 장에서 논의한 것과 동일한 방식으로 대화형 쿼리 서비스를 구축할 수 있습니다. 


### summary
- 데이터 창을 사용하면 이벤트 간의 시간적 관계를 도출할 수 있습니다
- 마지막으로 window 키-값 저장소를 쿼리하는 방법을 배우면 window 상태 저장소에 대해 중요한 사실을 알 수 있습니다. - 키는 다차원적(원래 키와 창의 시간 범위를 모두 포함)이므로 창 범위 스캔을 비롯한 다양한 쿼리 집합을 지원합니다.
- 다음 장에서는 고급 상태 관리 작업을 살펴봄으로써 상태 저장 Kafka Streams 응용 프로그램에 대한 논의를 마무리할 것입니다.







# CH6 Advance State Management
- 이 장의 목표는 state store를 더 깊이 파고들어 state store 스트림 처리 응용 프로그램을 빌드할 때 더 높은 수준의 안정성을 얻을 수 있도록 하는 것입니다.



- 우리가 대답할 몇 가지 질문은 다음과 같습니다.
  - 영구 상태 저장소는 디스크에 어떻게 표시됩니까? 
  - 상태 저장 응용 프로그램은 내결함성을 어떻게 달성합니까? 
  - 기본 제공 상태 저장소를 구성하려면 어떻게 해야 합니까? 
  - 상태 저장 응용 프로그램에 가장 영향을 미치는 이벤트 종류는 무엇입니까? 
  - 상태 저장 작업의 복구 시간을 최소화하기 위해 어떤 조치를 취할 수 있습니까? 
  - state store 이 무한정 성장하지 않도록 하려면 어떻게 해야 합니까? 
  - DSL 캐시를 사용하여 다운스트림 업데이트를 속도 제한하는 방법은 무엇입니까?
  - state restore listner 를 사용하여 상태 복원의 진행 상황을 어떻게 trace합니까? 
  - state 리스너를 사용하여 재조정을 감지하는 방법은 무엇입니까?



### Persistent Store Disk Layout

- Kafka Streams에는 in memory 와 영구 상태 저장소가 모두 포함되어 있습니다.
- 응용 프로그램의 복구 시간을 줄이는 데 도움이 될 수 있기 때문에 일반적으로 선호됩니다
- 기본적으로 영구 상태 저장소는 /tmp/ka€a-streams 디렉토리에 있습니다. (재부팅 또는 충돌시 tmp 폴더는 삭제되기때문에 다른위치 선택해야됨)
- 영구 상태 저장소는 디스크에 있기 때문에 파일을 매우 쉽게 검사할 수 있습니다


- [예제 6-1]의 파일 트리는 이전 장에서 만든 환자 모니터링 응용 프로그램에서 가져왔습니다.


1. 최상위 디렉토리에는 애플리케이션 ID가 있습니다. 이는 특히 여러 노드(예: Kubernetes 클러스터)에서 워크로드를 예약할 수 있는 공유 환경에서 서버에서 실행 중인 애플리케이션을 이해하는 데 유용합니다.
2. 각 두 번째 수준 디렉터리는 단일 Kafka Streams 작업에 해당합니다. 디렉터리 이름은 작업 ID로 형식이 지정됩니다. 작업 ID는  <sub-topology-id>_<partition> 의 두 부분으로 구성됩니다. 37페이지의 "하위 토폴로지"에서 논의한 것처럼 하위 토폴로지는 프로그램 논리에 따라 하나 또는 여러 주제의 데이터를 처리할 수 있습니다.
3. 검사점 파일은 변경 로그 항목의 오프셋을 저장합니다(178페이지의 "변경 로그 항목" 참조). 그들은 Kafka Streams에 어떤 데이터가 로컬 상태 저장소로 읽혀졌는지 알려주고 곧 보게 되겠지만 상태 저장소 복구에서 중요한 역할을 합니다.
4. 잠금 파일은 Kafka Streams에서 상태 디렉터리에 대한 잠금을 획득하는 데 사용됩니다. 이는 동시성 문제를 방지하는 데 도움이 됩니다.
5. 실제 데이터는 명명된 상태 디렉터리에 저장됩니다. 여기서 펄스 수는 상태 저장소를 구체화할 때 설정한 명시적 이름에 해당합니다.

- lock 파일과 체크포인트 파일은 특히 중요하며 특정 오류 로그에서 참조됩니다. 스트림이 잠금을 획득하지 못하는 경우), 위치와 유틸리티를 이해하는 것이 도움이 됩니다. 
- check point 파일은 상태 저장소 복구에서 중요한 역할을 합니다.



### Fault Tolerance

- Kafka Streams는 내결함성 특성의 대부분을 Kafka의 스토리지 계층 및 그룹 관리 프로토콜에 의존하고 있습니다. 예를 들어, 파티션 수준에서 데이터 복제는 브로커가 오프라인이 되어도 다른 브로커의 복제된 파티션 중 하나에서 데이터를 계속 사용할 수 있음을 의미합니다. 또한 소비자 그룹을 사용하면 애플리케이션의 단일 인스턴스가 다운될 경우 정상 인스턴스 중 하나로 작업이 재분배됩니다.

- 그러나 상태 저장 응용 프로그램의 경우 Kafka Streams는 응용 프로그램이 오류에 대한 복원력을 갖도록 추가 조치를 취합니다. 여기에는 상태 저장소를 지원하기 위한 change log topic(back state stores), and standby replicas (재초기화 시간을 최소화하기 위해)



### Changelog Topics


- 전체 상태가 손실되는 경우(또는 새 인스턴스를 시작하는 경우) 변경 로그 항목이 처음부터 재생됩니다. 그러나 체크포인트 파일이 존재하는 경우(예 6-1 참조), 이 오프셋은 상태 저장소로 이미 읽은 데이터를 나타내기 때문에 해당 파일에서 발견된 체크포인트 오프셋에서 상태를 재생할 수 있습니다.

- 예를 들어, 이전 장에서 다음 코드를 사용하여 pulse-counts라는 상태 저장소를 구체화했습니다.
- Materialized 클래스에는 changelog 주제를 더욱 커스터마이징할 수 있는 몇 가지 추가 메서드가 있습니다. 예를 들어 변경 로깅을 완전히 비활성화하려면 다음 코드를 사용하여 임시 저장소(즉, 실패 시 복원할 수 없는 상태 저장소)라고 하는 것을 생성할 수 있습니다

- 그러나 변경 로깅을 비활성화하는 것은 일반적으로 상태 저장소가 더 이상 내결함성이 없고 대기 복제본을 사용하지 못하게 하기 때문에 좋은 생각이 아닙니다. 변경 로그 주제 구성과 관련하여 더 일반적으로 withRetention 메소드를 사용하여 window 또는 세션 저장소의 보존을 무시하거나 변경 로그에 대한 특정 주제 구성을 전달합니다. 

  1. 주제 구성을 저장하기 위한 맵을 만듭니다. 항목에는 유효한 주제 구성 및 값이 포함될 수 있습니다.
  2.  Materialized.withLoggingEnabled 메소드에 주제 구성을 전달하여 변경 로그 주제를 구성하십시오.


- 이제 describe topic시, 토픽이 그에 따라 구성되었음을 알 수 있습니다

- 변경 로그 항목의 명명 체계는 <application_id>-<internal_store_name>--changelog입니다.
- 한 가지 유의할 점은 이 글을 쓰는 시점에서 변경 로그 항목이 생성된 후에는 이 방법을 사용하여 변경 로그 항목을 재구성할 수 없다는 것입니다. 기존 변경 로그 항목에 대한 항목 구성을 업데이트해야 하는 경우 다음을 수행합니다. Kafka 콘솔 스크립트를 사용하여 그렇게 해야 합니다.

  1. kafka-topics 콘솔 스크립트를 사용할 수도 있습니다. Confluent Platform 외부에서 Kafka를 실행하는 경우 파일 확장자(.sh)를 추가하는 것을 잊지 마십시오. 
  2. 토픽 컨피그레이션을 업데이트하십시오.


### Standby Replicas
- 스테이트풀 어플리케이션의 오류시 가동 중지 시간을 줄이는 한 가지 방법은 여러 응용 프로그램 인스턴스 복사본을 만들고 유지 관리하는 것입니다.
- NUM_STANDBY_REPLICAS_CONFIG 값을 입력시 카프카가 알아서 replica를 구성합니다.
- 이렇게 하면 기본 상태 저장소를 처음부터 다시 초기화할 필요가 없으므로 상태가 큰 응용 프로그램의 가동 중지 시간이 크게 줄어듭니다
-  let’s discuss what rebalancing is, and why it is the biggest enemy of stateful Kafka Streams applications.


### Rebalancing: Enemy of the State (Store)
- 전자(change log topics)를 사용하면 Kafka Streams가 상태가 손실될 때마다 상태를 재구성할 수 있고 후자(replicas)는 상태 저장소를 다시 초기화하는 데 걸리는 시간을 최소화할 수 있습니다. 
- 그러나 losing a state storeㄴ는 잠시의 시간이라도 매우 파괴적입니다. (특히 거대한 스테이트풀 시스템일수록)
- 상태 백업을 위한 체인지로깅토픽은 기존 토픽의 각 메시지를 재생해야 하고 해당 토픽이 큰 경우 각 레코드를 다시 읽는 데 몇 분 또는 극단적인 경우 몇 시간이 걸릴 수 있기 때문입니다.
- 상태를 재초기화하는 가장 큰 원인은 reblanacing입니다. 
- 우리는 13페이지의 "컨슈머 그룹"에서 컨슈머 그룹에 대해 논의할 때 이 용어를 처음 접했습니다. 간단한 설명은 Kafka가 컨슈머 그룹의 활성 구성원 전체에 작업을 자동으로 배포하지만 때로는 작업을 다음으로 재배포해야 한다는 것입니다. 

  1. 그룹 코디네이터는 컨슈머 그룹의 멤버십을 유지하는 책임이 있는 지정된 브로커입니다(예: 하트비트를 수신하고 멤버십 변경이 감지되면 재조정을 트리거). 
  2. 그룹 리더는 파티션 할당을 결정하는 책임이 있는 각 소비자 그룹의 지정된 소비자입니다.



- 현재로서는 리밸런싱으로 인해 stateful task가 스탠바이 레플리카가 없는 다른 인스턴스로 마이그레이션될 때 재조정이 비용이 많이 듭니다.  재조정으로 인해 발생할 수 있는 문제를 처리하기 위한 몇 가지 전략이 있습니다.
  1. 가능하면 상태가 이동하지 않도록 합니다. 
  2. 상태를 이동하거나 재생해야 하는 경우 가능한 한 빨리 복구 시간을 만듭니다.

- 위 두전략과 함께, Kafka Streams가 자동으로 취하는 조치와 스스로 취할 수 있는 조치가 있습니다. 




### Preventing State Migration
- stateful tasks가 실행 중인 다른 인스턴스에 재할당되면 기본 상태도 마이그레이션됩니다. 
- 상태가 큰 응용 프로그램의 경우 대상 노드에서 상태 저장소를 다시 작성하는 데 오랜 시간이 걸릴 수 있으므로 가능하면 피해야 합니다.
- 그룹 리더(소비자 중 하나)는 활성 소비자 간에 작업이 분산되는 방식을 결정하는 책임이 있으므로 Kafka Streams 라이브러리(로드 밸런싱 논리를 구현)에서 불필요한 상태 저장소 마이그레이션을 방지해야 합니다


### Sticky Assignment
- 스테이트불 테스크가 재할당되는 것을 방지하기 위해 Kafka Streams는 이전에 작업을 소유한 인스턴스에 작업을 재할당하려고 시도하는 사용자 지정 파티션 할당 전략5을 사용합니다(따라서 기본 상태 저장소의 복사본이 있어야 함). 이 전략을 스티키 어사인먼트라고 합니다.

- 그림 6-2에서 볼 수 있듯이 이는 잠재적으로 큰 상태 저장소의 불필요한 재초기화를 방지하는 데 도움이 되므로 응용 프로그램의 가용성을 크게 향상시킵니다.
- 스티키 어사이너는 reassign task를 이전 소유자에게 재할당하는 데 도움이 되지만 Kafka Streams 클라이언트가 일시적으로 오프라인 상태인 경우 상태 저장소를 마이그레이션할 수 있습니다. 
- 이제 Kafka Streams 개발자로서 일시적인 다운타임 동안 재조정을 방지하는 데 도움이 될 수 있는 것에 대해 논의해 보겠습니다.



### Static Membership
- state가 이동되어서 야기될 수있는 문제 중 하나는 불필요한 재조정입니다. 
- 인스턴스가 잠시 다운된 후 다시 온라인 상태가 되면 해당 인스턴스의 구성원 ID(consumer 등록될 때마다 코디네이터가 할당한 고유 식별자)가 지워지기 때문에 코디네이터는 이를 인식하지 못하므로 인스턴스는 새로운 구성원과 작업이 재배정될 수 있습니다.
- 이를 방지하기 위해 정적 멤버십을 사용할 수 있습니다. 정적 멤버십은 일시적인 다운타임으로 인한 재조정 횟수를 줄이는 것을 목표로 합니다.
  1. 이 경우 ID를 app-1로 설정합니다. 다른 인스턴스를 가동하면 고유 ID도 할당합니다(예: app-2). 이 ID는 전체 클러스터에서 고유해야 합니다(application.id와 독립적인 다른 Kafka Streams 응용 프로그램 간에도 포함).

- 하드코딩된 인스턴스 ID는 일반적으로 더 높은 세션 시간 초과와 함께 사용되며6 이는 애플리케이션을 다시 시작하는 데 더 많은 시간을 소요하므로 코디네이터는 소비자 인스턴스가 짧은 기간 동안 오프라인 상태가 될 때 죽은 것으로 생각하지 않습니다. 


### Reducing the Impact of Rebalances
- s shown in Figure 6-3. 이 재조정 전략을 eager rebalancing이라고 하며 두 가지 이유로 영향을 미칩니다. 
  1. 모든 클라이언트가 리소스를 포기할 때 소위 stop-the-world 효과가 발생합니다. 응용 프로그램은 처리가 중단되기 때문에 작업이 매우 빠르게 지연될 수 있습니다.
  2. stateful task가 새 인스턴스에 재할당되면 처리가 시작되기 전에 상태를 재생/재구축해야 합니다. 이로 인해 추가 가동 중지 시간이 발생합니다.

- 그러나 버전 2.4부터 재조정 프로토콜에 대한 업데이트는 재조정의 영향을 줄이는 데 도움이 되는 추가 조치를 도입했습니다. (두번째 문제를 완화하기위해)


### Incremental Cooperative Rebalancing
- Incremental Cooperative Rebalancing은 eager rebalancing보다 효율적인 재조정 프로토콜이며 2.4 이상 버전에서 기본적으로 활성화됩니다.

1. Rebalancing의 한 글로벌 라운드는 여러 개의 더 작은 라운드(incremental)로 대체됩니다. 
2. 클라이언트는 소유권을 변경할 필요가 없는 리소스(taskas)를 보유하고 마이그레이션 중인 작업(cooperative)만 처리를 중지합니다.


### Controlling State Size
- 주의하지 않으면 state store가 무한정 성장하여 운영 문제를 일으킬 수 있습니다
- 압축된 change log topic의 키 공간이 매우 크고(10억 개의 키가 있다고 가정해 봅시다) 애플리케이션 상태가 10개의 물리적 노드에 고르게 분포되어 있다고 상상해 보십시오.8 노드 중 하나가 오프라인이 되면 재생해야 합니다. 상태를 재구축하려면 최소 1억 개의 레코드가 필요합니다. 이것은 많은 시간이 걸리고 가용성 문제로 이어질 수 있습니다.

-  state store를 작게 유지하기 위해 불필요한 데이터를 제거하면 재조정의 영향이 크게 줄어듭니다. 상태 저장 작업을 마이그레이션해야 하는 경우 불필요하게 큰 상태 저장소보다 작은 상태 저장소를 다시 빌드하는 것이 훨씬 쉽습니다. 그렇다면 Kafka Streams에서 불필요한 상태를 어떻게 제거합니까? 우리는 Tombstones를 사용합니다



### Tombstones
- 삭제 표시는 일부 상태를 삭제해야 함을 나타내는 특수 레코드입니다. 삭제 마커라고도 하며 항상 키와 null 값을 갖습니다. 이전에 언급했듯이 상태 저장소는 키 기반이므로 삭제 표시 레코드의 키는 상태 저장소에서 삭제해야 하는 레코드를 나타냅니다.
- 이 가상 시나리오에서 우리는 병원 환자에 대해 일부 집계를 수행하고 있지만 환자 체크아웃 이벤트가 표시되면 이 환자에 대한 추가 이벤트가 더 이상 예상되지 않으므로 기본 상태 저장소에서 해당 데이터를 제거합니다.

  1. 환자가 체크아웃할 때마다 삭제 표시(즉, 삭제 마커)를 생성하려면 null을 반환합니다. 이로 인해 관련 키가 기본 상태 저장소에서 제거됩니다. 
  2. 환자가 체크아웃하지 않은 경우 집계 논리를 수행합니다. 삭제 표시는 키-값 저장소를 작게 유지하는 데 유용하지만 창 키-값 저장소에서 불필요한 데이터를 제거하는 데 사용할 수 있는 또 다른 방법이 있습니다. 다음 섹션에서 이에 대해 논의하겠습니다.


### window retention
- Windowed 저장소에는 상태 저장소를 작게 유지하기 위해 구성 가능한 보존 기간이 있습니다.
  1. 이 값은 명시적으로 재정의되지 않으므로 기본 보존 기간(1일)으로 window store를 구체화합니다.

- Materialized 클래스에는 Kafka Streams가 window 저장소에 레코드를 보관해야 하는 기간을 지정하는 데 사용할 수 있는 withRetention이라는 추가 메서드가 있습니다. 다음 코드는 window state의 보존을 지정하는 방법을 보여줍니다.

- 리텐션기간은 항상 the window size and thegrace period combined 보다 커야합니다. 
- 기본 변경 로그 주제를 작게 유지하는 또 다른 방법인 적극적인 주제 압축을 살펴보겠습니다.


### Aggressive topic compaction
- 기본적으로 변경 로그 항목은 압축됩니다. 즉, 각 키의 최신 값만 유지되며 삭제 표시를 사용하면 관련 키의 값이 완전히 삭제됩니다. 그러나 상태 저장소는 압축되거나 삭제된 값을 즉시 반영하지만 기본 토픽은 압축되지 않은/삭제된 값을 더 오랜 기간 동안 유지함으로써 필요한 것보다 더 크게 유지될 수 있습니다.
- 카프카 내부에선 토픽을 좀더 작게 추상화한 세그먼트라는 것이 존재, 토픽 파티션에 대한 메시지의 서브세트를 포함하는 파일입니다
- 이는 현재 기본 파티션에 대해 기록되고 있는 파일입니다. 시간이 지남에 따라 활성 세그먼트는 크기 임계값에 도달하여 비활성화됩니다. 세그먼트가 비활성화된 경우에만 청소 대상이 됩니다.



-  아래 표에 나열된 topic configuration은 보다 적극적인 압축을 가능하게 하여 상태 저장소를 다시 초기화해야 하는 경우 재생해야 하는 레코드를 줄이는 데 유용합니다.
  1. 이 구성은 로그의 세그먼트 파일 크기를 제어합니다. 정리는 항상 한 번에 파일로 수행되므로 세그먼트 크기가 클수록 파일 수가 줄어들지만 보존에 대한 세부적인 제어가 줄어듭니다.
  2. 이 구성은 이전 데이터를 압축하거나 삭제할 수 있도록 세그먼트 파일이 가득 차지 않았더라도 Kafka가 로그를 강제로 롤링하는 시간을 제어합니다.
  3. 이 구성은 로그 압축 프로그램이 로그 정리를 시도하는 빈도를 제어합니다(로그 압축이 활성화되어 있다고 가정). 기본적으로 로그의 50% 이상이 압축된 로그를 정리하지 않습니다
  4. 메시지가 로그에서 압축할 수 없는 최대 시간입니다. 압축 중인 로그에만 적용됩니다.
  5. 메시지가 로그에 압축되지 않은 상태로 유지되는 최소 시간입니다. 압축 중인 로그에만 적용됩니다.

- 이러한 항목 구성은 세그먼트 크기와 청소 가능한 최소 더티 비율을 줄여 보다 빈번한 로그 청소를 트리거하는 데 도움이 될 수 있습니다.


### Fixed-size LRU cache

- 상태 저장소가 무한정 커지지 않도록 하는 덜 일반적인 방법은 메모리 내 LRU 캐시를 사용하는 것입니다. 이것은 상태가 구성된 크기를 초과할 때 가장 덜 최근에 사용된 항목을 자동으로 삭제하는 구성 가능한 고정 용량(최대 항목 수로 지정)이 있는 간단한 키-값 저장소입니다. 

1. 최대 크기가 10개 항목인 counts라는 메모리 내 LRU 저장소를 만듭니다. 
2. 스토어 공급자를 사용하여 인메모리 LRU 스토어를 구체화합니다.

-  복원 시간은 상태 저장소를 다시 초기화하기 위해 전체 주제를 재생해야 하기 때문에 영구 상태 저장소보다 더 길 수 있습니다(LRU 맵에서 10개의 레코드로만 해결되더라도!).. 이것은 101페이지의 "영구적 메모리 대 메모리 내 저장소"에서 처음 논의한 메모리 내 저장소 사용의 주요 단점이므로 이 옵션은 장단점을 완전히 이해하고 사용해야 합니다.

- . 이제 읽기 대기 시간이나 쓰기 볼륨으로 인해 상태 저장소에 병목 현상이 발생하는 경우 추구할 수 있는 전략을 살펴보겠습니다.


### Deduplicating Writes with Record Caches
- 기본 상태 저장소와 다운스트림 프로세서 모두에 상태 업데이트가 기록되는 빈도를 제어하기 위한 작동 매개변수가 있습니다
1. 모든 스레드에서 버퍼링에 사용할 최대 메모리 양(바이트)
2. 프로세서의 위치를 저장할 빈도


- 더 큰 캐시 크기와 더 높은 커밋 간격은 동일한 키에 대한 연속적인 업데이트를 중복 제거하는 데 도움이 될 수 있습니다. 다음과 같은 몇 가지 이점이 있습니다.
  1. 읽기 대기 시간 감소 
  2. 쓰기 볼륨 감소: 
    — 상태 저장소 
    — 기본 변경 로그 주제(활성화된 경우) 
    — 다운스트림 스트림 프로세서



- 따라서 병목 현상이 상태 저장소에 대한 읽기/쓰기 또는 네트워크 I/O(변경 로그 항목에 대한 빈번한 업데이트의 부산물일 수 있음)로 나타나는 경우 이러한 매개변수 조정을 고려해야 합니다. 
- 물론 더 큰 레코드 캐시에는 몇 가지 단점이 있습니다.
  1. 더 높은 메모리 사용량 
  2. 더 높은 대기 시간(레코드가 덜 자주 방출됨)

- 더 큰 커밋 간격을 사용하는 것과도 상충 관계가 있습니다. 즉, 이 구성의 값을 높이면 실패 후 다시 수행해야 하는 작업의 양이 늘어납니다.


### state store monitoring
- 애플리케이션을 프로덕션에 배포하기 전에 애플리케이션을 적절하게 지원할 수 있도록 애플리케이션에 대한 충분한 가시성을 확보하는 것이 중요합니다. 이 섹션에서는 상태 저장 응용 프로그램을 모니터링하는 일반적인 접근 방식에 대해 논의하여 운영 수고를 줄이고 오류가 발생했을 때 디버깅할 수 있는 충분한 정보를 얻을 수 있도록 합니다.

### Adding State Listeners
- Kafka Streams 애플리케이션은 여러 상태 중 하나에 있을 수 있습니다(상태 저장소와 혼동하지 말 것). 그림 6-5는 이러한 각 상태와 변할수 있는 상태 보여줍니다.
- 이전에 언급했듯이 재조정 상태는 상태 저장 Kafka Streams 애플리케이션에 특히 영향을 미칠 수 있으므로 애플리케이션이 재조정 상태로 전환되는 시점과 이러한 상황이 발생하는 빈도를 추적할 수 있으면 모니터링 목적에 유용할 수 있습니다. 
- 다행스럽게도 Kafka Streams는 State Listener라는 것을 사용하여 애플리케이션 상태가 변경될 때 모니터링하는 것을 매우 쉽게 만듭니다. 상태 리스너는 단순히 애플리케이션 상태가 변경될 때마다 호출되는 콜백 메서드입니다.


  1. KafkaStreams.setStateListener 메서드를 사용하여 애플리케이션 상태가 변경될 때마다 메서드를 호출합니다. 
  2. StateListener 클래스의 메서드 서명에는 이전 상태와 새 상태가 모두 포함됩니다. 
  3. 응용 프로그램이 재조정 상태에 들어갈 때마다 조건부로 일부 작업을 수행합니다.

- 상태리스너가 유일한 리스너는 아니며 또다른 리스너를 살펴보자

### Adding State Restore Listeners
- Kafka Streams에는 상태 저장소가 다시 초기화될 때마다 호출할 수 있는 상태 복원 수신기라고 하는 또 다른 수신기가 포함되어 있습니다


1. onRestoreStart 메소드는 상태 재초기화를 시작할 때 호출됩니다. StartingOffset 매개변수는 전체 상태를 재생해야 하는지 여부를 나타내기 때문에 특히 중요합니다(이는 재초기화의 가장 영향력 있는 유형이며 메모리 내 저장소를 사용할 때 발생하거나 영구 저장소를 사용할 때 발생하며 이전 상태는 다음과 같습니다. 잃어버린). startingOffset이 0으로 설정되면 전체 재초기화가 필요합니다. 0보다 큰 값으로 설정하면 부분 복원만 필요합니다. 
2. onRestoreEnd 메소드는 복원이 완료될 때마다 호출됩니다. 
3. onBatchRestored 메소드는 단일 배치의 레코드가 복원될 때마다 호출됩니다. 배치의 최대 크기는 MAX_POLL_RECORDS 구성과 동일합니다. 이 메서드는 잠재적으로 여러 번 호출될 수 있으므로 이 메서드에서 동기 처리를 수행할 때는 복원 프로세스가 느려질 수 있으므로 주의해야 합니다. 나는 일반적으로 이 방법에서 아무 것도 하지 않습니다(심지어 로깅조차도 매우 시끄러울 수 있음).

### Built-in Metrics





### Interactive Queries

1. 특정 키가 있는 경우 해당 키가 있어야 하는 호스트 및 포트 쌍을 포함하는 지정된 키에 대한 메타데이터를 가져옵니다.
2.  활성 Kafka Streams 인스턴스의 호스트 이름을 추출합니다.
3.  활성 Kafka Streams 인스턴스의 포트를 추출합니다.

1. KafkaStreams.queryMetadataForKey 메서드를 사용하여 지정된 키에 대한 활성 호스트와 대기 호스트를 모두 가져옵니다. 
2. 활성 호스트가 살아 있는지 확인하십시오. 이를 직접 구현해야 하지만 잠재적으로 상태 수신기(196페이지의 "상태 수신기 추가" 참조)와 해당 API 끝점을 RPC 서버에 추가하여 응용 프로그램의 현재 상태를 표시할 수 있습니다. isAlive는 애플리케이션이 Running 상태일 때마다 true로 확인되어야 합니다.
3. 활성 호스트가 활성 상태가 아니면 복제된 상태 저장소 중 하나를 쿼리할 수 있도록 대기 호스트를 검색합니다. 참고: 대기가 구성되지 않은 경우 이 메서드는 빈 집합을 반환합니다. 

### 컷텀 스테이트 스토어
-  자신의 상태 저장소를 구현하는 것도 가능합니다. 이렇게 하려면 StateStore 인터페이스를 구현해야 합니다. 
- 마지막으로 사용자 지정 저장소를 구현하기로 결정했다면 네트워크 호출이 필요한 모든 저장소 솔루션이 잠재적으로 성능에 큰 영향을 미칠 수 있다는 점에 유의하십시오. RocksDB 또는 로컬 인메모리 저장소가 좋은 선택인 이유 중 하나는 스트림 작업과 함께 배치되기 때문입니다

### summary

- 이제 Kafka Streams에서 상태 저장소를 내부적으로 관리하는 방법과 상태 저장 애플리케이션이 시간이 지남에 따라 원활하게 실행되도록 하기 위해 개발자가 사용할 수 있는 옵션에 대해 더 깊이 이해해야 합니다. 여기에는 삭제 표시 사용, 적극적인 주제 압축 및 상태 저장소에서 오래된 데이터를 제거하기 위한 기타 기술이 포함됩니다(따라서 상태 재초기화 시간 단축). 또한 대기 복제본을 사용하여 상태 저장 작업에 대한 장애 조치 시간을 줄이고 재조정이 발생할 때 애플리케이션의 고가용성을 유지할 수도 있습니다. 마지막으로, 리밸런싱은 영향력이 있지만 정적 멤버십을 사용하여 어느 정도 방지할 수 있으며 증분 협력 리밸런싱이라는 개선된 리밸런싱 프로토콜을 지원하는 Kafka Streams 버전을 사용하여 영향을 최소화할 수 있습니다.






# 7장 프로세서 API

### start
- 이 장에서는 Kafka Streams에서 사용할 수 있는 낮은 수준의 API인 Processor API(PAPI라 고도 함)를 살펴보겠습니다.
- Processor API는 고급 DSL보다 추상화가 적고 명령형 프로그래 밍 방식을 사용합니다. 코드는 일반적으로 더 장황하지만 더 강력하여 다음 사항에 대한 세밀한 control을 부여합니다.
데이터가 토폴로지를 통해흐르는방식,스트림프로세서가서로관련되는방식,상태가생성및유지되는방식,특정작 업의 타이밍.

- 이장이 끝나면 다음과 같은 질문에 답변 가능합니다.
  • 언제 프로세서 API를 사용해야 합니까?
  • 프로세서 API를 사용하여 소스, 싱크 및 스트림 프로세서를 어떻게 추가합니까? •정기행사를어떻게예약할수있습니까?
  • 프로세서 API를 상위 수준 DSL과 혼합할 수 있습니까?
  • 프로세서와 트랜스포머의 차이점은 무엇입니까?


### When to Use the Processor API

- 일반적으로 프로젝트에 복잡성을 도입 할 때마다 그럴만 한 이유가 있어야합니다. Processor API가 불필요 하게 복잡하지는 않지만 낮은 수준의 특성(DSL 및 ksqlDB에 비해)과 더 적은 추상화로 인해 더많은코드와주의하지않으면더많은실수가발생할수있습니다.

- 일반적으로 다음 중 하나를 활용해야 하는 경우 프로세서 API를 활용할 수 있습니다.
  • 레코드 메타데이터에 대한 액세스(주제, 파티션, 오프셋 정보, 레코드 헤더 등)
  • 일정 스케쥴링 기능
  • 레코드가 다운스트림 프로세서로 전달되는 시기에 대한 보다 세밀한 제어
  • state store에 대한 보다 세분화된 액세스 
  • DSL에서발생하는모든제한사항을우회하는기능(나중에이에대한예를볼수있음)

- 반면에 Processor API를 사용하면 다음과 같은 몇 가지 단점이 있을 수 있습니다.
• 더 많은 코드로인해 유지보수 비용증가 및 가독성 저하
• 다른프로젝트 관리자에게 더 높은 진입장벽
• DSL 기능 또는 추상화의 우발적인 재창조, exotic problem-framing , 성능 함정요소

- 다행히 Kafka Streams를 사용하면 애플리케이션에서 DSL과 프로세서 API를 모두 혼합할 수 있으므로 어느 쪽을 선택해도 모두 들어갈 필요가 없습니다



### Introducing Our Tutorial: IoT Digital Twin Service
- 디지털 트윈 물리적 개체의 상태가 디지털 사본에 반영되는 것입니다.
- 우리는 40개의 풍력 터빈이 있는 풍력 발전 단지가 있습니다. 터빈 중하나가현재상태(풍속,온도,전력상태등)를 report 할 때마다해당정보를 키‐값저장소에 저장합니다.
- 이를 통해 한 장치의 report/desire 상태 이벤트를 다른 장치와 구별할 수 있습니 다.

- 예를들어 전원을 on/off 하는경우 직접 신호를 터빈에 보내는 대신에, 우리는 desired를 setting 할 수 있다. 


1. Kafka 클러스터에는 두 가지 주제가 포함되어 있으므로 Processor API를 사용하여 소스 프로 세서를 추가하는 방법을 배워야 합니다. 다음은 이러한 주제에 대한 설명입니다.
• 각 풍력 터빈 (에지 노드)에는 일련의 환경 센서가 장착되어 있으며이 데이터 (예 : 풍속)는 터빈 자체에 대한 일부 메타 데이터 (예 : 전력 상태)와 함께 reported state 이벤트 주기적으로 주제.
• desire-state-events는 사용자 또는 프로세스가 터빈의 전원상태를 변경하려고 할 때마다 작성됩니다(즉, 전원을 끄거나 켤 때).

2. 환경 센서 데이터는 reported state events topic에서 주어진 터빈에 대해 보고된 풍속이 안전한 작동 수준을 초과하는지 여부를 결정 하는 스트림 프로세서를 추가합니다. 만약 그렇다면 자동으로 종료 신호를 생성합니다. 프로세 서 API를 사용하여 상태 비저장 스트림 프로세서를 추가하는 방법을 알려줍니다.

3. 세 번째 단계는 두 부분으로 나뉩니다.
• 첫째, 두가지 유형의 이벤트(reported event/desire event)가 디지털 트윈 레코드로 결합됩니다. 이러한 레코드는 처리된 다음 이라는 퍼시스턴트 키‐값 저장소에 기록됩 니다. 디지털트윈스토어라 불리는. 이 단계에서는 프로세서 API를 사용하여 상태 저장소에 연결 하고 상호 작용하는 방법과 DSL을 통해 액세스할 수 없는 특정 레코드 메타데이터에 액세스하는 방법을 배웁니다.
• 이단계의 두번째 부분은 스케쥴링을 포함한 기능이다, punctuator(구두점) 이라불리는, 7일 이상 업데이트되지 않은 오래된 디지털 트윈 레코드를 정리합니다. 프로 세서 API의 구두점 인터페이스를 소개하고 상태 저장소에서 키를 제거하는 대체 방법 도 보여줍니다.

4. 각 디지털 트윈 레코드는 digital-twins라는 토픽에 씌여집니다.. 이 단계에서는 프로세서 API를 사용하여 싱크 프로세서를 추가하는 방법 을 배웁니다.

5. Kafka Streams의 대화형 쿼리 기능을 통해 디지털 트윈 레코드를 노출합니다. 몇 초마다 풍력 터빈의 마이크로 컨트롤러는 자체 상태를 Kafka Streams에 의해 노출 된 원하는 상태와 동기화하려고 시도합니다. 예를 들어 2단계에서 종료 신호를 생성하면 (desired power state를 off로 설정), 그러면 터빈은 Kafka Streams 앱을 쿼리할 때 이 원하는 상태를 보고 블레이드의 전원을 차단합니다.



### Data Models
- 평소와 같이 토폴로지 생성을 시작하기 전에 먼저 데이터 모델을 정의합니다
- 튜토리얼 개요에서 언급했듯이 보고된 상태 레코드와 원하는 상태 레코드를 결합하여 디지털 트윈 레코드를 생성해야 합니다. 따라서 결합된 레코드에 대한 데이터 클래스도 필요합니다















-------
# TROUBLE Shooting
1.로컬에서 kafka-broker-api-versions으로 카프카브로커와 통신하려고 했는데 runtime exception이 남
- security group에서 inbound 9092 포트열어줌

2.카프카 클러스터구성후 실행시 이미 한번 카프카실행했던 01번 브로커에서 clusterId가 meta 파일과 일치하지 않는다고 나옴
해결방법 : 
카프카 설정파일(카프카 설치 디렉토리/config/server.properties)의 log.dirs 항목에서 설정되어 있는 카프카 로그 path로 가보면 meta.properties라는 파일이 있는데 해당 파일을 지워주고 카프카를 재시작해주면 해결된다.
카프카 설치 디렉토리/config/server.properties 파일 

3.클러스터 구성시에 streams로 topic을 복사를하니 브로커01에는 절대로 복사가 되지않았다 스트림즈는 어디로복사를하게될까?

4.책에서 만든대로 topology에서 source stream을 읽으려는데 계속 Gson관련 오류가남 (topology가 실행되려면 토폴로지 실행후 데이터를 흘려보내줘야함)
(.json 파일이 prettier가 실행되면서 포맷이변환되면서 생기는문제, saveaction ignore파일을추가하면됨)

# 궁금점.
1.record의 키값을 붙여서 보내면 같은 파티션으로 저장되게 되는데, 이것의 의미는 무엇일까? 단순히 column - value 느낌은 아닌것 같다. (처음에 key라는게 column 이라고 생각)


* iterm 단축키
Cmd + d 화면세로분할
Cmd + shift + d 가로분할
Cmd + w 탭 닫기
Cmd + / 탭분할시 포커스 찾기
Cmd + , 탭분할시 포커스 이동
Cmd + shift + h 클립보드
Cmd + ; 자동완성
Cmd + shift + e 작업시간보여주기
Cmd + shift + i 여러창 동시입력
Ctrl + u , k line delete k는 뒷라인 제거
Ctrl + a , e 커서 맨앞 맨뒤
command+shift+option 드래그 다시 탭바합치기
option+cmd+i 멀티인풋
ctrl+cmd+shi+op+i 현재 커서 멀티인풋제외

* vi 단축키
^ - 문장 맨 앞으로 이동
0 - 라인 맨 앞으로 이동
$ - 문장 맨 뒤로 이동
I - 현재 라인 맨앞넣기
A - 라인 맨뒤넣기
O,o - 윗줄 아랫줄에 insert하기
