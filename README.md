MOSH is an eclipse plugin. It uses model execution and reinforcement learning to efficiently test self-healing CPSs (SH-CPSs) under uncertainty. In MOSH, the expected behaviors of the SH-CPS under test are specified as executable UML state machines. Via an extended version of Moka (a Papyrus based UML model execution plugin) MOSH can execute the models together with the system under test. By checking the system’s actual behaviors against the expected ones, MOSH manages to find faults in the SH-CPS. 

On the other hand, MOSH uses a reinforcement learning based algorithm to learn the likelihood that a fault can be detected, i.e., the system under test is going to fail, after invoking a given operation. The likelihood is defined as fragility, and the highest discounted fragility after firing a transition is defined as the T-value of the transition. Directed by the T-value, MOSH manages to find the optimal operation invocations to effectively detect faults. 

