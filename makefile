ifeq (runMaster,$(firstword $(MAKECMDGOALS)))
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RUN_ARGS):;@:)
endif
ifeq (runStorage,$(firstword $(MAKECMDGOALS)))
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(RUN_ARGS):;@:)
endif


make:
	javac -encoding ISO-8859-1 -cp ./gson-2.8.5.jar \
	Client.java MasterServer.java StorageServer.java Message.java \
	MulticastServer.java FileVersion.java

runMaster:
	java -cp ./:./gson-2.8.5.jar \
	MasterServerTest

runStorage:
	java -cp ./:./gson-2.8.5.jar \
	StorageServerTest $(RUN_ARGS)

runClient:
	java -cp ./:./gson-2.8.5.jar \
	ClientTest $(RUN_ARGS)

rm:
	rm *.class
