INSTRUÇÕES DE COMPILAÇÃO E EXECUÇÃO

CRIAÇÃO DO DIRETÓRIO BIN:
    mkdir bin

----------------------------------------------------------------------------------------------------------------
*.JAR JÁ ESTÃO CRIADOS, NÃO É NECESSÁRIO CORRER ESTE CÓDIGO*

    COMPILAÇÃO DOS .JAVA PARA .CLASS:
    javac -Xlint -d bin -verbose -g src/src/ReadWrite/*.java
    javac -Xlint -cp jar_files/* -d bin/ -verbose -g src/src/GroupDataBase/Data/*.java
    javac -Xlint -cp jar_files/*;bin -d bin/ -verbose -g src/src/GroupDataBase/*.java
    javac -Xlint -cp jar_files/*;bin -d bin/ -verbose -g src/src/Trokos/app/*.java

----------------------------------------------------------------------------------------------------------------

----------------------------------------------------------------------------------------------------------------
*COMPILAÇÃO DE .CLASS PARA .JAR*:
    -> O nosso grupo utilizou as ferramentas do IntelliJ para criar as dependências relativas aos .jar e assim compilar
    e correr os .jar;
    -> No entanto abaixo temos os comandos que teoricamente compilariam os .jar, apesar de não conseguirmos criar as
    dependências;
    -> Recomendável não testar os comandos.


// Put to Jar - TrokosServer
jar cfm projeto-1.jar MANIFEST.MF bin/GroupDataBase bin/ReadWrite bin/Trokos/app

// Rename Projeto-1 to TrokosServer
ren projeto-1.jar TrokosServer.jar

// Put to Jar - Trokos
jar cfm projeto-1.jar client_manifest.txt bin/GroupDataBase bin/ReadWrite bin/Trokos/app

// Rename Projeto-1 to Trokos
ren projeto-1.jar Trokos.jar

// check jar file
jar tf projeto-1.jar
jar tf TrokosServer.jar
jar tf Trokos.jar
----------------------------------------------------------------------------------------------------------------

COMO CORRER OS .JAR:

-> INSTRUÇÕES:

    Argumentos geral:
     <keystore> : Nome do ficheiro guardado na pasta "keystores/";
     <password-keystore> : Password criada na criação da keystore.

    Argumentos Servidor:
     <port> : opcional (default=45678);
     <password-cifra> : Password usada para cifrar users.cif e bd.cif (usado para Alg. PBE).
    
    Argumentos Cliente:
     <serverAddress> = <IP:port> : 
        - IP : 127.0.0.1 - (obrigatório);
        - port : (default=45678) - (opcional);
     <truststore> : ficheiro onde se guarda o certificado auto-assinado do Servidor (usado para a ligação SSL);
     <userID> : ID do Cliente (igual ao alias passado na keystore do respetivo Cliente).



----------------------------------------------------------------------------------------------------------------
*KEYSTORES JÁ ESTÃO CRIADAS, ABAIXO SÓ EXPLICA COMO É FEITA A CRIAÇÁO*

        CRIAR KEYSTORE:

            Servidor: keytool -genkeypais -alias myServer -keyalg RSA -keysize 2048 -keystore keystore.server

            Cliente: keytool -genkeypais -alias <userID> -keyalg RSA -keysize 2048 -keystore <keystore>


        CRIAR TRUSTSTORE:

            Depois de se fazer export do certificado auto-assinado do Servidor e criadas as keystores:

                keytool -importcert -alias myServer -file certServer.cer -keystore truststore.client
------------------------------------------------------------------------------------------------------------------

CORRER SERVIDOR:

    Exemplo Servidor:
        Argumentos:
            <port> = 45678
            <password-cifra> = "cifrado"
            <keystore> = "keystore.server"
            <password-keystore> = "serverKeystore"

        Comando: "java -jar TrokosServer.jar 45678 cifrado keystore.server serverKeystore"


CORRER CLIENTE:

    Exemplo CLiente 1:
        Argumentos:
            <serverAddress> = <IP:port> = 127.0.0.1:45678
            <truststore> = "truststore.client"
            <keystore> = "keystore.rodrigo"
            <password-keystore> = "Rodrigo"
            <userID> = "Rodrigo"

        Comando: "java -jar Trokos.jar 127.0.0.1:45678 truststore.client keystore.rodrigo Rodrigo Rodrigo"

    Exemplo CLiente 2:
        Argumentos:
            <serverAddress> = <IP:port> = 127.0.0.1:45678
            <truststore> = "truststore.client"
            <keystore> = "keystore.vasco"
            <password-keystore> = "Vasco0"
            <userID> = "Vasco0"

        Comando: "java -jar Trokos.jar 127.0.0.1:45678 truststore.client keystore.vasco Vasco0 Vasco0"

    Exemplo CLiente 3:
        Argumentos:
            <serverAddress> = <IP:port> = 127.0.0.1:45678
            <truststore> = "truststore.client"
            <keystore> = "keystore.miguel"
            <password-keystore> = "Miguel"
            <userID> = "Miguel"

        Comando: "java -jar Trokos.jar 127.0.0.1:45678 truststore.client keystore.miguel Miguel Miguel"

COMANDOS NO TERMINAL:
    Servidor:   java -jar TrokosServer.jar <port> <password-cifra> <keystore> <password-keystore>
    Cliente:    java -jar Trokos.jar <serverAddress> <truststore> <keystore> <password-keystore> <userID>

LIMITAÇÕES:
    Transações limitadas a inteiros, ou seja 15/2=7;
    Trokos: Não possuímos handler para controlar o comando crl+c. Criámos comando "quit" ou "q" que desliga o socket.
    Se quiser reiniciar o programa do zero é necessário apagar os ficheiros users.cif, bd.cif, params, certificados em "certificates/" e blockChains na pasta "blockchain/".