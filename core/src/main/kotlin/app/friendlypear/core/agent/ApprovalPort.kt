package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact

/** What a guardian is asked to approve. Only the kind travels, never the message or the code. */
enum class ApprovalKind { SEND_CODE, CIRCLE_CHANGE }

enum class ApprovalResult { APPROVED, REJECTED, EXPIRED, FAILED }

/**
 * Family MFA. The phone creates a one-time link (15 minutes), texts it to the guardians with [smsFor] and,
 * when they decide or the link expires, calls [PeriAgent.onApprovalResult] with the same [ticket].
 * Silence is [ApprovalResult.EXPIRED]: nothing is ever approved by itself.
 */
interface ApprovalPort {
    fun request(ticket: String, kind: ApprovalKind, toName: String, guardians: List<Contact>, smsFor: (url: String) -> String)
}
