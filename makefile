make:
	javac -cp /home/eliot/Documents/Trimestre\ XIV/Sistemas\ Operativos\ II/\
	SistemaManejadorDeVersiones:/home/eliot/Documents/Trimestre\ XIV/\
	Sistemas\ Operativos\ II/SistemaManejadorDeVersiones/gson-2.8.5.jar \
	Client.java MasterServer.java StorageServer.java Message.java \
	MulticastServer.java FileVersion.java

runMaster:
	java -cp /home/eliot/Documents/Trimestre\ XIV/Sistemas\ Operativos\ II/\
	SistemaManejadorDeVersiones:/home/eliot/Documents/Trimestre\ XIV/\
	Sistemas\ Operativos\ II/SistemaManejadorDeVersiones/gson-2.8.5.jar \
	MasterServerTest

runStorage:
	java -cp /home/eliot/Documents/Trimestre\ XIV/Sistemas\ Operativos\ II/\
	SistemaManejadorDeVersiones:/home/eliot/Documents/Trimestre\ XIV/\
	Sistemas\ Operativos\ II/SistemaManejadorDeVersiones/gson-2.8.5.jar \
	StorageServerTest

runClient:
	java -cp /home/eliot/Documents/Trimestre\ XIV/Sistemas\ Operativos\ II/\
	SistemaManejadorDeVersiones:/home/eliot/Documents/Trimestre\ XIV/\
	Sistemas\ Operativos\ II/SistemaManejadorDeVersiones/gson-2.8.5.jar \
	ClientTest

rm:
	rm *.class