Kafka 실습
## 기본정보
1. kaf1 public - 18.116.164.235
2. kaf2 18.188.11.200
3. kaf3 52.14.217.250

## 카프카 세대 클러스터링

18.116.164.235 broker01
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
ssh -i edgar-kaf1.pem ec2-user@18.116.164.235
```


## 자바 설치
```bash
sudo yum install -y java-1.8.0-openjdk-devel.x86_64
java -version
```

## 주키퍼 카프카 브로커 실행
```bash
wget https://archive.apache.org/dist/kafka/2.5.0/kafka_2.12-2.5.0.tgz
tar xvf kafka_2.12-2.5.0.tgz
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
curl https://archive.apache.org/dist/kafka/2.5.0/kafka_2.12-2.5.0.tgz --output kafka.tgz
tar -xvf kafka.tgz
bin/kafka-broker-api-versions.sh --bootstrap-server 18.116.164.235:9092
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

-------
# TROUBLE Shooting
1.로컬에서 kafka-broker-api-versions으로 카프카브로커와 통신하려고 했는데 runtime exception이 남
- security group에서 inbound 9092 포트열어줌

2.카프카 클러스터구성후 실행시 이미 한번 카프카실행했던 01번 브로커에서 clusterId가 meta 파일과 일치하지 않는다고 나옴
해결방법 : 
카프카 설정파일(카프카 설치 디렉토리/config/server.properties)의 log.dirs 항목에서 설정되어 있는 카프카 로그 path로 가보면 meta.properties라는 파일이 있는데 해당 파일을 지워주고 카프카를 재시작해주면 해결된다.
카프카 설치 디렉토리/config/server.properties 파일 

3.클러스터 구성시에 streams로 topic을 복사를하니 브로커01에는 절대로 복사가 되지않았다 스트림즈는 어디로복사를하게될까?


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




