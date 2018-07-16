make:
	javac -encoding ISO-8859-1 -cp ./gson-2.8.5.jar \
	Client.java MasterServer.java StorageServer.java Message.java \
	MulticastServer.java FileVersion.java

runMaster:
	java -cp ./:./gson-2.8.5.jar \
	MasterServerTest

runStorage:
	java -cp ./:./gson-2.8.5.jar \
	StorageServerTest

runClient:
	java -cp ./:./gson-2.8.5.jar \
	ClientTest

rm:
	rm *.class
