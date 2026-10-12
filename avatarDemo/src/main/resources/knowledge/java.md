Java seekhte waqt beginners ka focus mostly syntax par hota hai:

- if
- for
- class
- object
- interface

Lekin Java ka real understanding syntax se nahi, balki ye samajhne se aata hai ki program actually execute kaise hota hai.
Java ka important idea hai:

```
Java Source Code
      ↓
Compiler
      ↓
Bytecode
      ↓
JVM
      ↓
Machine Instructions
```

Hum `.java` file likhte hain.
For example:

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello Java");
    }
}
```

Java compiler source code ko directly machine code mein convert nahi karta.
Instead:

```
Main.java
   ↓
javac
   ↓
Main.class
```

`Main.class` ke andar bytecode hota hai.
Phir JVM us bytecode ko execute karti hai.
Yahi reason hai ki Java ko hum commonly **"Write Once, Run Anywhere"** ke concept se relate karte hain.
Same bytecode theoretically different operating systems par run kar sakta hai, provided wahan compatible JVM available ho.

### Stack and Heap
Method execution aur local variables generally stack se related hote hain, jabki dynamically created objects heap memory mein exist karte hain.
Example:

```java
Student student = new Student();
```

Conceptually:
- `student` reference → **Stack**
- `Student` Object → **Heap**

Java deeply samajhne ke liye sirf syntax yaad karna enough nahi hai. Hume execution model, memory, objects aur JVM ko connect karke dekhna chahiye.

*My one-line Java rule is:* "Syntax Java ka surface hai, JVM Java ki story hai."