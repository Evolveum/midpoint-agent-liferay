rem TODO [mso] add dependecy with appropriate version to pom.xml when it is accessible from outside
call mvn install:install-file -DgroupId=com.evolveum.midpoint.model -DartifactId=model-client -Dversion=2.2-SNAPSHOT -Dpackaging=jar -Dfile=./model-client-2.2-SNAPSHOT.jar
pause