Placeholder Running guidelines

Ensure you are on projects/tutorials/akka/techdemo/demo6

sbt "runMain ApplicationRunner seed" -Dport_number=2551
sbt "runMain ApplicationRunner chief" -Dport_number=2552
sbt "runMain ApplicationRunner bastard" -Dport_number=2553

Dependency app
//python2 fixture.py -n 2000