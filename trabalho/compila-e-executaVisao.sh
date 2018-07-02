#!/bin/bash
#

#CONFIG="-Djgroups.bind_addr=172.16.2.222 -Djava.net.preferIPv4Stack=true"
CONFIG="-Djava.net.preferIPv4Stack=true"

LIBS=../lib/jgroups-3.6.4.Final.jar:./
export CLASSPATH=$CLASSPATH:$LIBS

javac Protocolo.java
javac Visao.java


RUN_CMD="java $CONFIG -cp $LIBS Visao 2>/dev/null"
echo "$RUN_CMD"

## executar 1 processo neste computador
xterm -hold -e "$RUN_CMD"  &
sleep 1

## executar outras 3 janelas neste computador
##xterm -hold -e "$RUN_CMD"  &
##xterm -hold -e "$RUN_CMD"  &
##xterm -hold -e "$RUN_CMD"  &

## OBS.: se nao tiver o xterm instalado, use o comando abaixo:
#x-terminal-emulator -e "$RUN_CMD"  &
#x-terminal-emulator -e "$RUN_CMD"  &
#x-terminal-emulator -e "$RUN_CMD"  &
