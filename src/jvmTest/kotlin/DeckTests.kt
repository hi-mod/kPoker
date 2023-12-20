import com.poker.domain.Card
import com.poker.domain.CardRank
import com.poker.domain.CardSuit
import com.poker.domain.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldNotBeEqualComparingTo
import io.kotest.matchers.shouldBe

internal class DeckTests : FunSpec({

    context("Deck Tests") {
        test("testLazyShuffledDeckIterator") {
            val deck = Deck()
            val card1 = deck.iterator().next()
            val card2 = deck.iterator().next()
            card1 shouldNotBeEqualComparingTo card2
        }

        test("testDeckContainsAllCards") {
            val deck = Deck()
            val allCards = CardRank.entries.flatMap { rank ->
                CardSuit.entries.map { suit -> Card(rank, suit) }
            }.toSet()
            deck.cards.toSet() shouldBe allCards
        }

        test("testShuffledDeckIsRandom") {
            val cards = Deck().toList()
            val expectedDistribution = 52 / (CardRank.entries.size * CardSuit.entries.size)
            val count = mutableMapOf<Pair<CardRank, CardSuit>, Int>()

            // Count the number of occurrences of each rank and suit in the array
            for (card in cards) {
                val key = Pair(card.rank, card.suit)
                count[key] = count.getOrDefault(key, 0) + 1
            }

            // Ensure that the number of occurrences of each rank and suit is close to the expected distribution
            for (rank in CardRank.entries) {
                for (suit in CardSuit.entries) {
                    val key = Pair(rank, suit)
                    val occurrences = count.getOrDefault(key, 0)
                    assert(occurrences >= expectedDistribution * 0.9 && occurrences <= expectedDistribution * 1.1)
                }
            }
        }
    }
})
