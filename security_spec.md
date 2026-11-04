# Security Specification — Mizan Smart Finance

## 1. Data Invariants
1. Every document in `/users/{userId}/transactions/{txId}`, `/users/{userId}/budgets/{budgetId}`, and `/users/{userId}/templates/{templateId}` must belong exclusively to `userId == request.auth.uid`.
2. Document IDs (`txId`, `budgetId`, `templateId`) must match `^[a-zA-Z0-9_\-]+$` with length <= 128.
3. All write payloads must include exact allowlisted keys (`hasAll` and `hasOnly`), valid string bounds, positive numbers, valid enum values (`type`, `priority`, `recurrence`, `wallet`), and server timestamps (`<= request.time`).
4. Immutable fields (`userId`, `createdAt`) cannot be modified on `update`.

## 2. The Dirty Dozen Payloads
1. Unauthenticated read/list on `/users/alice/transactions`.
2. Cross-user read (`bob` reading `/users/alice/transactions/tx1`).
3. Cross-user write (`bob` creating in `/users/alice/transactions/tx1`).
4. Identity spoofing (`alice` creating in `/users/alice/transactions/tx1` with `userId: "bob"`).
5. Shadow/ghost field injection (`isVerified: true` on transaction create or update).
6. Oversized string DoS (`title` > 120 chars or `category` > 60 chars).
7. Invalid enum value (`priority: "unknown"` or `wallet: "crypto"`).
8. Negative or zero transaction amount (`amount: -50`).
9. Future timestamp spoofing (`createdAt` > `request.time`).
10. Immutable field mutation (`createdAt` or `userId` modified on `update`).
11. Invalid document ID poisoning (`txId` containing spaces or special characters).
12. Wrong type injection (`monthlyLimit: "1000"` as string instead of number).
