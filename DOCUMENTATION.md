# RebuildPak — Technical Documentation

**Package:** `com.example.madproject`  
**Version:** 1.0 (versionCode 1)  
**Platform:** Android (minSdk 24 · targetSdk 36 · compileSdk 36)

---

## Overview

RebuildPak is a construction contractor marketplace for Pakistan. Clients post jobs, contractors submit bids, jobs are awarded and managed end-to-end with tasks, materials, payments, reviews, and real-time chat. Google Gemini AI is integrated throughout for smart assistance.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| UI | XML layouts · Material Components 1.12 · ConstraintLayout · CardView |
| Backend | Firebase Firestore (NoSQL) |
| Auth | Firebase Auth (Email/Password + Phone OTP) |
| Storage | Firebase Storage (job photos, portfolio images) |
| Messaging | Firebase Cloud Messaging (FCM) push notifications |
| AI | Google Generative AI SDK (`generativeai:0.1.2`) — Gemini 1.5 Flash |
| Image loading | Glide 4.16.0 |
| Background jobs | WorkManager 2.9.0 |
| Circle images | CircleImageView (hdodenhof) |

---

## Firebase Dependencies (BOM 34.7.0)

```
firebase-auth · firebase-firestore · firebase-storage
firebase-messaging · firebase-bom
```

---

## Screens (31 Activities)

| Activity | Description |
|---|---|
| `SplashActivity` | Entry point — routes to login or dashboard |
| `MainActivity` | Login screen (email/password) |
| `SignupActivity` | Registration — client or contractor, profile setup |
| `ClientDashboardActivity` | Client home — recent jobs, stats, quick actions |
| `ContractorDashboardActivity` | Contractor home — available jobs, my bids, earnings |
| `AvailableJobsActivity` | Browse open jobs — search, city filter, budget filter, sort |
| `JobPostActivity` | Post a new job — photos, AI description generator, permit check, contractor advice |
| `JobDetailActivity` | View job — bid list, accept/reject bids, mark complete, rate contractor, AI contract + budget tips |
| `JobEditActivity` | Edit an open job |
| `MyJobsActivity` | Client's posted jobs list |
| `MyProjectsActivity` | Contractor's active/completed projects |
| `SubmitBidActivity` | Submit bid on a job — AI cost estimate |
| `ContractorDirectoryActivity` | Browse contractors — category pills, rating/experience filter, sort |
| `ContractorProfileActivity` | View contractor profile, portfolio, reviews |
| `TaskListActivity` | Task board for a project — tabs, status counts, AI site report |
| `TaskDetailActivity` | Task detail — update progress, mark complete, AI safety tips |
| `AddTaskActivity` | Create a new task — AI timeline estimate |
| `MaterialManagementActivity` | View materials for a project |
| `AddMaterialActivity` | Add material — category, quantity, price, AI suggestions |
| `AllReviewsActivity` | All reviews for a contractor |
| `PortfolioGalleryActivity` | Contractor portfolio images |
| `ChatActivity` | 1-on-1 real-time messaging |
| `ConversationsListActivity` | All chat conversations |
| `AIChatActivity` | Multi-turn AI assistant chat — reset conversation, suggestion chips |
| `NotificationsActivity` | In-app notification feed |
| `EditProfileActivity` | Edit profile — photo, name, skills, phone verify badge |
| `PhoneVerificationActivity` | Firebase OTP phone verification flow |
| `SettingsActivity` | Change password, notification toggles, privacy/terms/help, logout |
| `PaymentActivity` | Payment flow — deposit (30%) and final (70%) |

---

## Data Models

### User
```
userId, email, fullName, phoneNumber, userType (client|contractor)
category, skills, experience, location, bio, profileImageUrl
rating, completedProjects, totalEarnings
phoneVerified (bool), pushNotificationsEnabled (bool), messageNotificationsEnabled (bool)
createdAt, updatedAt
```

### Job
```
jobId, clientId, clientName, title, description, category, location
budget, timeline, status (open|in_progress|completed|cancelled)
totalBids, assignedContractorId, assignedContractorName, acceptedBidId, acceptedBidAmount
depositPaid (bool), finalPaid (bool), postedDate, updatedAt
photoUrls (List<String>)
```

### Bid
```
bidId, jobId, jobTitle, contractorId, contractorName
bidAmount, completionDays, proposal, status (pending|accepted|rejected)
contractorCategory, contractorRating, contractorCompletedProjects
submittedDate
```

### Task
```
taskId, jobId, projectName, taskTitle, description, assignedTo
numberOfWorkers, estimatedQuantity, completedQuantity, progressUnit
dailyWages, status (not_started|ongoing|completed)
startDate, endDate, createdBy, createdAt, updatedAt
```

### Material
```
materialId, jobId, projectName, materialName, category
quantity, unit, unitPrice, totalCost, supplier, description
addedBy, addedDate
```

### Review
```
reviewId, contractorId, contractorName, clientId, clientName
jobId, jobTitle, rating (float), reviewText, createdAt
```

### Payment
```
paymentId, jobId, clientId, contractorId
amount, type (deposit|final), status (pending|completed|failed)
transactionId, createdAt
```

### Notification
```
notificationId, userId, title, message, type (bid|job|payment|message)
referenceId, isRead, createdAt
```

### Message / Conversation
```
messageId, conversationId, senderId, receiverId, text, timestamp, isRead
conversationId, participants (List<String>), lastMessage, lastMessageTime, unreadCount
```

### ChatMessage (local AI chat)
```
message, isUser (bool), timestamp
```

---

## Firebase Architecture

### Firestore Collections

```
/users/{userId}
/jobs/{jobId}
/bids/{bidId}
/tasks/{taskId}
/materials/{materialId}
/reviews/{reviewId}
/payments/{paymentId}
/notifications/{notificationId}
/messages/{messageId}
/conversations/{conversationId}
```

### Recommended Indexes
- `bids`: `jobId ASC, submittedDate DESC`
- `tasks`: `jobId ASC, updatedAt DESC`
- `notifications`: `userId ASC, createdAt DESC`
- `messages`: `conversationId ASC, timestamp ASC`

---

## Firebase Manager Singletons

All managers are singletons accessed via `getInstance()`.

| Manager | Key Methods |
|---|---|
| `UserManager` | `createUser`, `getUserObject`, `updateField`, `updateRating`, `searchContractors` |
| `JobManager` | `createJob`, `getJob`, `getJobsByClient`, `assignContractor`, `completeJob`, `incrementTotalBids` |
| `BidManager` | `createBid`, `getBidsByJob`, `acceptBid`, `rejectBid`, `rejectOtherBids`, `checkExistingBid` |
| `TaskManager` | `createTask`, `getTask`, `getTasksByJob`, `updateProgress`, `completeTask` |
| `MaterialManager` | `createMaterial`, `getMaterialsByJob` |
| `ReviewManager` | `createReview`, `getReviewsByJob`, `calculateAverageRating` |
| `PaymentManager` | `createPayment`, `updatePaymentStatus` |
| `NotificationManager` | `createNotification`, `getNotificationsForUser`, `markAsRead` |
| `MessageManager` | `sendMessage`, `getMessages`, `getConversations`, `markConversationRead` |

---

## AI Features — 11 Total (Google Gemini 1.5 Flash)

All AI features live in `GeminiAIHelper`. One-shot methods use `generateContent()`; chat uses `ChatFutures` for multi-turn history.

| # | Method | Button | Screen |
|---|---|---|---|
| 1 | `sendMessage()` | Chat input | `AIChatActivity` — multi-turn, full session history, reset button in toolbar |
| 2 | `helpWriteJobDescription()` | ✨ AI Generate | `JobPostActivity` — generates and fills description field |
| 3 | `getContractorRecommendation()` | 🔍 Contractor Advice | `JobPostActivity` — recommends contractor type for the job |
| 4 | `checkPermitRequirements()` | 📋 Permit Check | `JobPostActivity` — lists NOCs/approvals for category + city (CDA, LDA, KDA etc.) |
| 5 | `getConstructionEstimate()` | ✨ AI Cost Estimate | `SubmitBidActivity` — PKR cost breakdown (materials, labor, equipment) |
| 6 | `getMaterialRecommendations()` | ✨ AI Suggest | `AddMaterialActivity` — lists materials with quantities and estimated PKR costs |
| 7 | `getTimelineEstimate()` | ⏱ AI Estimate | `AddTaskActivity` — task duration estimate with phases |
| 8 | `getSafetyTips()` | ⚠️ AI Safety Tips | `TaskDetailActivity` — top 5 safety tips, required PPE, hazards |
| 9 | `generateContract()` | 📄 Generate Contract | `JobDetailActivity` — formal client-contractor contract, shareable via intent |
| 10 | `getBudgetOptimizationTips()` | 💡 Budget Optimization | `JobDetailActivity` — 5 cost-saving strategies, local sourcing tips |
| 11 | `generateProgressReport()` | 📊 AI Report | `TaskListActivity` — executive summary from task stats, risk flags, shareable |

### System Context (prepended to all prompts)
> "You are an AI assistant for RebuildPak, a construction marketplace in Pakistan. Help with: cost estimates (PKR), timelines, materials, safety, contractor advice, job descriptions. Be concise (2-3 paragraphs max), use PKR for costs, consider Pakistani standards and local material prices."

---

## Key User Flows

### Client Flow
1. Signup as client → `ClientDashboardActivity`
2. Post job (`JobPostActivity`) — add photos, AI description, check permits
3. View bids (`JobDetailActivity`) — sort by lowest/highest/recent
4. Accept bid → `in_progress`, contractor notified, 30% deposit card shown
5. Pay deposit → `PaymentActivity`
6. Monitor tasks → `TaskListActivity` → generate AI site report
7. Mark job complete → contractor notified → 70% final payment card shown
8. Rate contractor via review dialog (RatingBar + text)
9. Generate AI contract during `in_progress` / `completed`

### Contractor Flow
1. Signup → select category, skills, experience
2. Browse jobs (`AvailableJobsActivity`) — filter by city, budget, sort
3. Submit bid (`SubmitBidActivity`) — use AI cost estimate as reference
4. Bid accepted → notified → appears in `MyProjectsActivity`
5. Manage tasks (`TaskListActivity`) — add tasks, AI timeline estimates
6. Update task progress (`TaskDetailActivity`) — AI safety tips per task
7. Add materials (`AddMaterialActivity`) — AI material suggestions

### Phone Verification Flow
1. Edit Profile → tap "Verify" pill next to phone field
2. `PhoneVerificationActivity` — `PhoneAuthProvider.verifyPhoneNumber()`
3. Auto-verify on SIM match, else 6-digit OTP input
4. 60-second countdown + resend
5. `linkWithCredential()` → `UserManager.updateField("phoneVerified", true)` → green badge

### AI Chat Flow
1. Open AI Assistant → `emptyState` hidden, welcome message shown, suggestion chips visible
2. Tap chip or type message → user bubble added, spinner shown
3. `ChatFutures.sendMessage()` — history maintained across turns
4. System context prepended on first message only
5. Reset button (toolbar) → `resetConversation()` → fresh `ChatFutures` session

---

## Notification Triggers

| Event | Recipient | Type |
|---|---|---|
| Bid submitted | Job owner (client) | `bid` |
| Bid accepted | Contractor | `bid` |
| Job marked complete | Contractor | `job` |
| New chat message | Recipient | `message` (FCM via `MyFirebaseMessagingService`) |

---

## Filter & Sort Systems

### Available Jobs (`AvailableJobsActivity`)
- Search: title substring (client-side)
- Category: Spinner (15 categories)
- City: Spinner (14 major Pakistani cities)
- Budget: min/max dialog (button highlights orange when active)
- Sort: Newest · Oldest · Budget ↑ · Budget ↓ · Most Bids

### Contractor Directory (`ContractorDirectoryActivity`)
- Search: name substring (client-side)
- Category: pill chips (6 trade categories)
- Rating: Any / 4+★ / 4.5+★
- Experience: Any / 1+ / 3+ / 5+ years
- Sort: Highest Rated · Most Projects · Most Experienced · Newest

---

## Background Work

### `CloseExpiredJobsWorker` (WorkManager)
- Periodic job scheduled from `MadProjectApplication.onCreate()`
- Queries `open` jobs past deadline → updates status to `cancelled`

### `MyFirebaseMessagingService`
- Handles incoming FCM push notifications
- Builds local notification, opens relevant screen on tap

---

## Settings

| Setting | Implementation |
|---|---|
| Change Password | `EmailAuthProvider.getCredential()` → `reauthenticate()` → `updatePassword()` |
| Push Notifications | Switch → `UserManager.updateField("pushNotificationsEnabled", bool)` |
| Message Notifications | Switch → `UserManager.updateField("messageNotificationsEnabled", bool)` |
| Logout | Confirmation dialog → `FirebaseAuth.signOut()` → `MainActivity` with `FLAG_ACTIVITY_CLEAR_TASK` |

---

## Project Setup

### 1. Firebase
1. Create project at console.firebase.google.com
2. Add Android app (`com.example.madproject`)
3. Download `google-services.json` → place in `app/`
4. Enable: Authentication (Email + Phone), Firestore, Storage, Cloud Messaging

### 2. Gemini API Key
1. Get key from aistudio.google.com
2. Add to `local.properties` (never commit this file):
   ```
   GEMINI_API_KEY=your_key_here
   ```

### 3. Build & Run
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Firestore Security Rules (recommended)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }

    match /jobs/{jobId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth.uid == resource.data.clientId;
    }

    match /bids/{bidId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth.uid == resource.data.contractorId
                    || request.auth.uid == get(/databases/$(database)/documents/jobs/$(resource.data.jobId)).data.clientId;
    }

    match /tasks/{taskId} {
      allow read, write: if request.auth != null;
    }

    match /materials/{materialId} {
      allow read, write: if request.auth != null;
    }

    match /reviews/{reviewId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth.uid == resource.data.clientId;
    }

    match /notifications/{notificationId} {
      allow read, update: if request.auth.uid == resource.data.userId;
      allow create: if request.auth != null;
    }

    match /messages/{messageId} {
      allow read, write: if request.auth != null;
    }

    match /conversations/{conversationId} {
      allow read, write: if request.auth != null;
    }

    match /payments/{paymentId} {
      allow read: if request.auth.uid == resource.data.clientId
                  || request.auth.uid == resource.data.contractorId;
      allow write: if request.auth != null;
    }
  }
}
```

---

## Package Structure

```
com.example.madproject/
├── [31 Activity classes]
├── adapters/
│   ├── BidAdapter, ChatMessageAdapter, ContractorAdapter
│   ├── ConversationAdapter, JobAdapter, MaterialAdapter
│   ├── MessageAdapter, NotificationAdapter, PortfolioAdapter
│   ├── ReviewAdapter, SelectedPhotoAdapter, TaskAdapter
├── firebase/
│   ├── BidManager, JobManager, MaterialManager, MessageManager
│   ├── NotificationManager, PaymentManager, ReviewManager
│   ├── TaskManager, UserManager
├── helpers/
│   ├── FCMHelper
│   └── GeminiAIHelper          ← 11 AI features
├── models/
│   ├── Bid, ChatMessage, Conversation
│   ├── GeminiRequest, GeminiResponse
│   ├── Job, Material, Message
│   ├── Notification, Payment, Review, Task, User
├── services/
│   └── MyFirebaseMessagingService
└── workers/
    └── CloseExpiredJobsWorker
```

---

*Last updated: 2026-07-18*
