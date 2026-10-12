High-Level Design mein hum individual classes se zoom out karte hain.
Question change ho jaata hai.
- LLD poochta hai: *Code ke andar components kaise design honge?*
- HLD poochta hai: *Pura system kaise kaam karega?*

Suppose hume YouTube jaisa platform design karna hai.
Immediately database choose karna correct starting point nahi hai.
Sabse pehle requirements samjho:
- Users video upload karenge
- Users video stream karenge
- Millions of users ho sakte hain
- Videos large files hain
- Traffic read-heavy ho sakta hai

Ab architecture derive karna start kar sakte hain:
`Client` $\rightarrow$ `Load Balancer` $\rightarrow$ `Application Servers` $\rightarrow$ `Database`

Video files ke liye:
`Application` $\rightarrow$ `Object Storage` $\rightarrow$ `CDN` $\rightarrow$ `Users`

Jaise-jaise scale badhta hai, naye problems appear hote hain:
- High Traffic
- Caching
- Database Bottlenecks
- Replication
- Partitioning
- Message Queues
- Fault Tolerance

System design ka goal fancy technologies use karna nahi hai.
Har technology ko kisi actual problem ka solution hona chahiye.
Redis sirf isliye architecture mein nahi aana chahiye kyunki Redis popular hai.
Kafka sirf isliye nahi aana chahiye kyunki large companies Kafka use karti hain.
Pehle bottleneck identify karo, phir technology introduce karo.

*My one-line HLD rule is:* "Hmesha pehle MVP bnao uska Architecture diagram bnao and then iteratively better karo"