#!/bin/bash
#

#CONFIG="-Djgroups.bind_addr=172.16.2.222 -Djava.net.preferIPv4Stack=true"
CONFIG="-Djava.net.preferIPv4Stack=true"

LIBS=../lib/jgroups-3.6.4.Final.jar:./
export CLASSPATH=$CLASSPATH:$LIBS

javac Nickname_List.java
javac Sala_List.java
javac State.java
javac Persistencia.java

RUN_CMD="java $CONFIG -cp $LIBS Persistencia 2>/dev/null"
echo "$RUN_CMD"

xterm -hold -e "$RUN_CMD"  &
