package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact

/** What a guardian is asked to approve. Only the kind travels, never the message or the code. */
enum class ApprovalKind { SEND_CODE, CIRCLE_CHANGE }

/** [CALL_BACK]: the guardian would rather talk first; Peri offers to call them. */
enum class ApprovalResult { APPROVED, REJECTED, CALL_BACK, EXPIRED, FAILED }

/**
 * Family MFA. The phone creates a one-time link (15 minutes), texts it to the guardians with [smsFor] and,
 * when they decide or the link expires, calls [PeriAgent.onApprovalResult] with the same [ticket].
 * Silence is [ApprovalResult.EXPIRED]: nothing is ever approved by itself.
 * [onCall] tells the guardian the person is on the phone with a stranger right now (a scam in progress), as a
 * yes or no: never the number, the call or the message.
 */
interface ApprovalPort {
    fun request(ticket: String, kind: ApprovalKind, toName: String, guardians: List<Contact>, onCall: Boolean, smsFor: (url: String) -> String)
}
