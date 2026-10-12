Spring ko samajhne ke liye sabse pehle ye samajhna zaroori hai ki Spring kis problem ko solve karta hai.
Normal Java application mein hum objects khud create kar sakte hain:

```java
PaymentService paymentService = new PaymentService();
OrderService orderService = new OrderService(paymentService);
```

Small application mein ye perfectly fine hai. Problem tab start hoti hai jab application mein hundreds of classes aur dependencies aa jaati hain.
Agar har class apni dependencies khud create karegi, toh classes tightly coupled ho jaayengi.

Spring ka core idea hai:
**Object creation aur dependency management application code se bahar nikal do.**

Spring ek container provide karta hai jo objects create karta hai, unki dependencies identify karta hai aur required objects ko inject karta hai.
Isi idea ko hum Inversion of Control (IoC) kehte hain.

Normally:
`Object` $\rightarrow$ `creates dependency`

Spring ke saath:
`Spring Container` $\rightarrow$ `creates objects` $\rightarrow$ `injects dependencies`

Dependency Injection IoC ko implement karne ka ek common way hai.
For example:

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

`OrderService` ko ye concern nahi hai ki `PaymentService` ka object kaise create hoga. Woh simply apni requirement declare karta hai.
Spring container baaki ka kaam manage karta hai.

### My Teaching Analogy
Main IoC ko restaurant analogy se explain karta hoon.
Restaurant mein jaakar hum kitchen setup nahi karte, chef hire nahi karte aur ingredients arrange nahi karte.
Hum simply meal order karte hain.
Restaurant poori creation process manage karta hai.

Isi tarah Spring mein object apni dependency khud create nahi karta. Woh dependency maangta hai aur Spring container usse provide karta hai.

*My one-line Spring rule is:* "Meal order karo, kitchen mat banao."