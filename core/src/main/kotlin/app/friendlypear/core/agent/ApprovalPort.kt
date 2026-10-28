package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact

/** What a guardian is asked to approve. Only the kind travels, never the message or the code. */
enum class ApprovalKind { SEND_CODE, CIRCLE_CHANGE }

/** [CALL_BACK]: the guardian would rather talk first; Peri offers to call them. */
enum class ApprovalResult { APPROVED, REJECTED, CALL_BACK, EXPIRED, FAILED }

/**
 * The riskiest requests need two guardians' yes, when two or more are asked: changing the guardians, or a code while a
 * stranger is (or just was) on the line. The family's server applies the same rule (backend/src/approvals/store.ts).
 */
fun ApprovalKind.needsTwo(onCall: Boolean, guardians: Int): Boolean = guardians >= 2 && (this == ApprovalKind.CIRCLE_CHANGE || onCall)

/**
 * Family MFA. The phone creates a one-time link per guardian (15 minutes), texts each its own with [smsFor] and,
 * as they answer, calls [PeriAgent.onApprovalProgress] (a first yes of two) and [PeriAgent.onApprovalResult] when
 * they decide or the links expire, with the same [ticket].
 * Silence is [ApprovalResult.EXPIRED]: nothing is ever approved by itself.
 * [onCall] tells the guardian the person is on the phone with a stranger right now (a scam in progress), as a
 * yes or no: never the number, the call or the message.
 */
interface ApprovalPort {
    fun request(ticket: String, kind: ApprovalKind, toName: String, guardians: List<Contact>, onCall: Boolean, smsFor: (url: String) -> String)
}
