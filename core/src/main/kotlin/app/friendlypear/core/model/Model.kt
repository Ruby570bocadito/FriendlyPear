package app.friendlypear.core.model

enum class Channel { SMS, WHATSAPP, EMAIL, BROWSER_NOTIFICATION }

/** The language Peri speaks: the phone's. Messages are checked in both, since scams arrive in either. */
enum class Lang { ES, EN }

/** Shield verdict levels: Peri stays quiet, asks to check, or warns. */
enum class Level { OK, DOUBT, SCAM }

/** How a contact relates to the protected person, seen from the person. */
enum class Relation { CHILD, GRANDCHILD, NEPHEW_NIECE, SIBLING, PARTNER, FRIEND, CAREGIVER, OTHER }

/** Trust levels of the family circle, from most to least trusted. */
enum class TrustLevel { GUARDIAN, VERIFIED_CONTACT, OFFICIAL_ENTITY, ACQUAINTANCE, UNKNOWN }

data class Contact(
    val id: String,
    val name: String,
    val phones: List<String>,
    val relation: Relation? = null,
    val aliases: List<String> = emptyList(),
    val trust: TrustLevel = TrustLevel.ACQUAINTANCE,
)

data class IncomingMessage(
    val channel: Channel,
    /** What the notification shows as sender: a saved name, a number, an SMS sender ID or an email. */
    val sender: String,
    val text: String,
    val receivedAtMillis: Long,
)
