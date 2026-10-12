Low-Level Design ko main directly design patterns se start nahi karta.
Pehla question hona chahiye: **Hum actually design kya kar rahe hain?**

LLD ka focus generally software ke internal structure par hota hai:
- Classes
- Objects
- Interfaces
- Responsibilities
- Relationships
- Design Patterns
- Extensibility

Suppose hume parking lot system design karna hai.
Beginner immediately classes banana start kar sakta hai:
- `Car`
- `Bike`
- `ParkingLot`
- `Ticket`
- `Payment`

Lekin classes banane se pehle requirements samajhna important hai:
- Different vehicle types hain?
- Different parking spots hain?
- Ticket generate hoga?
- Payment kaise hoga?
- Entry aur exit kaise handle hongi?

Requirements se responsibilities emerge hoti hain.
Then hum decide karte hain ki kaunsi class kis responsibility ko own karegi.

Good LLD ka purpose maximum classes banana nahi hai.
Purpose hai responsibilities ko cleanly separate karna.

### Example
Agar `ParkingLot` class:
- parking manage kare
- payment kare
- ticket print kare
- notification send kare
- database access kare
toh ek hi class bahut zyada responsibilities handle kar rahi hai.

Instead responsibilities split ki ja sakti hain:
- `ParkingLot`
- `TicketService`
- `PaymentService`
- `NotificationService`

SOLID principles aur design patterns tab useful hote hain jab hume actual design problem samajh aa chuki ho. Pattern ko force nahi karna chahiye.

*My one-line LLD rule is:* "If DSA is the brain of Application, then LLD is the skeleton."