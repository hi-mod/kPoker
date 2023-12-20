import com.poker.domain.Card
import com.poker.domain.CardRank
import com.poker.domain.CardSuit
import com.poker.domain.HandKind
import com.poker.domain.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class PlayerTests : FunSpec({

    fun createRoyalFlushHand(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.Ace, CardSuit.Spades), Card(CardRank.King, CardSuit.Spades)))

        val board = listOf(
            Card(CardRank.Queen, CardSuit.Spades),
            Card(CardRank.Jack, CardSuit.Spades),
            Card(CardRank.Ten, CardSuit.Spades),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Spades),
        )

        return Pair(player, board)
    }

    fun createStraightFlushHand(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.Queen, CardSuit.Spades), Card(CardRank.King, CardSuit.Spades)))

        val board = listOf(
            Card(CardRank.Seven, CardSuit.Spades),
            Card(CardRank.Jack, CardSuit.Spades),
            Card(CardRank.Ten, CardSuit.Spades),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Spades),
        )
        return Pair(player, board)
    }

    fun createFlushHand(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.Two, CardSuit.Spades), Card(CardRank.Three, CardSuit.Spades)))

        val board = listOf(
            Card(CardRank.Seven, CardSuit.Spades),
            Card(CardRank.Five, CardSuit.Spades),
            Card(CardRank.Ten, CardSuit.Spades),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Spades),
        )
        return Pair(player, board)
    }

    fun createHandQuads(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.Ace, CardSuit.Spades), Card(CardRank.Ace, CardSuit.Clubs)))

        val board = listOf(
            Card(CardRank.Ace, CardSuit.Diamonds),
            Card(CardRank.Ace, CardSuit.Hearts),
            Card(CardRank.Six, CardSuit.Spades),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Spades),
        )
        return Pair(player, board)
    }

    fun createFullHouseHand(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.Ace, CardSuit.Spades), Card(CardRank.Ace, CardSuit.Clubs)))

        val board = listOf(
            Card(CardRank.Ace, CardSuit.Diamonds),
            Card(CardRank.Jack, CardSuit.Spades),
            Card(CardRank.Jack, CardSuit.Clubs),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Spades),
        )
        return Pair(player, board)
    }

    fun createStraightHand(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.King, CardSuit.Spades), Card(CardRank.Ace, CardSuit.Clubs)))

        val board = listOf(
            Card(CardRank.Queen, CardSuit.Hearts),
            Card(CardRank.Jack, CardSuit.Diamonds),
            Card(CardRank.Ten, CardSuit.Spades),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Spades),
        )
        return Pair(player, board)
    }

    fun createThreeOfAKindHand(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.Ace, CardSuit.Hearts), Card(CardRank.Ace, CardSuit.Clubs)))

        val board = listOf(
            Card(CardRank.Ace, CardSuit.Diamonds),
            Card(CardRank.Jack, CardSuit.Spades),
            Card(CardRank.Ten, CardSuit.Spades),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Spades),
        )
        return Pair(player, board)
    }

    fun createTwoPairHand(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.Two, CardSuit.Spades), Card(CardRank.Two, CardSuit.Clubs)))

        val board = listOf(
            Card(CardRank.Three, CardSuit.Diamonds),
            Card(CardRank.Three, CardSuit.Spades),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Diamonds),
            Card(CardRank.Ten, CardSuit.Spades),
        )
        return Pair(player, board)
    }

    fun createOnePairHand(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.Ace, CardSuit.Hearts), Card(CardRank.Ace, CardSuit.Clubs)))

        val board = listOf(
            Card(CardRank.Two, CardSuit.Diamonds),
            Card(CardRank.Jack, CardSuit.Spades),
            Card(CardRank.Ten, CardSuit.Spades),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Spades),
        )
        return Pair(player, board)
    }

    fun createHighCardHand(): Pair<Player, List<Card>> {
        val player = Player("", "", 0.0, mutableListOf(Card(CardRank.Three, CardSuit.Hearts), Card(CardRank.Ace, CardSuit.Clubs)))

        val board = listOf(
            Card(CardRank.Two, CardSuit.Diamonds),
            Card(CardRank.Jack, CardSuit.Spades),
            Card(CardRank.Ten, CardSuit.Spades),
            Card(CardRank.Nine, CardSuit.Spades),
            Card(CardRank.Eight, CardSuit.Spades),
        )
        return Pair(player, board)
    }

    context("handDescription returns the correct hand description") {
        test("Royal Flush") {
            val (player, board) = createRoyalFlushHand()

            val expected = HandKind.RoyalFlush.description
            val actual = player.handDescription(board)
            expected shouldBe actual
        }

        test("Straight Flush") {
            val (player, board) = createStraightFlushHand()

            val expected = HandKind.StraightFlush.description + ", King to Nine"
            val actual = player.handDescription(board)
            expected shouldBe actual
        }

        test("Quads") {
            val (player, board) = createHandQuads()

            val expected = HandKind.FourOfAKind.description + ", Aces"
            val actual = player.handDescription(board)
            expected shouldBe actual
        }

        test("Full House") {
            val (player, board) = createFullHouseHand()

            val expected = HandKind.FullHouse.description + ", Aces full of Jacks"
            val actual = player.handDescription(board)
            expected shouldBe actual
        }

        test("Flush") {
            val (player, board) = createFlushHand()

            val expected = HandKind.Flush.description + ", Ten high"
            val actual = player.handDescription(board)
            expected shouldBe actual
        }

        test("Straight") {
            val (player, board) = createStraightHand()

            val expected = HandKind.Straight.description + ", Ten to Ace"
            val actual = player.handDescription(board)
            expected shouldBe actual
        }

        test("Three Of A Kind") {
            val (player, board) = createThreeOfAKindHand()

            val expected = HandKind.ThreeOfAKind.description + ", Aces"
            val actual = player.handDescription(board)
            expected shouldBe actual
        }

        test("Two Pair") {
            val (player, board) = createTwoPairHand()

            val expected = HandKind.TwoPair.description + ", Threes and Deuces"
            val actual = player.handDescription(board)
            expected shouldBe actual
        }

        test("One Pair") {
            val (player, board) = createOnePairHand()

            val expected = HandKind.Pair.description + " of Aces"
            val actual = player.handDescription(board)
            expected shouldBe actual
        }

        test("High Card") {
            val (player, board) = createHighCardHand()

            val expected = HandKind.HighCard.description + " Ace"
            val actual = player.handDescription(board)
            expected shouldBe actual
        }
    }
})

/*
    @Test
    fun handRankRoyalFlush() {
        val player = Player("", 0.0)
        val board = createRoyalFlushHand(player)

        val expected = HandKind.RoyalFlush.rank
        val actual = player.handRank(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handKindRoyalFlush() {
        val player = Player("", 0.0)
        val board = createRoyalFlushHand(player)

        val expected = HandKind.StraightFlush
        val actual = player.handKind(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handRankStraightFlush() {
        val (player, board) = createStraightFlushHand()

        val expected = HandKind.StraightFlush.rank
        val actual = player.handRank(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handKindStraightFlush() {
        val (player, board) = createStraightFlushHand()

        val expected = HandKind.StraightFlush
        val actual = player.handKind(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handRankQuads() {
        val (player, board) = createHandQuads()

        val expected = HandKind.FourOfAKind
        val actual = player.handKind(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handRankFullHouse() {
        val player = Player("", 0.0)
        player.addCard(Card(Rank.Ace, Suit.Spades))
        player.addCard(Card(Rank.Ace, Suit.Clubs))

        val board = listOf(Card(Rank.Ace, Suit.Diamonds),
                           Card(Rank.Jack, Suit.Spades),
                           Card(Rank.Jack, Suit.Clubs),
                           Card(Rank.Nine, Suit.Spades),
                           Card(Rank.Eight, Suit.Spades))

        val expected = HandKind.FullHouse
        val actual = player.handKind(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handRankFlush() {
        val (player, board) = createFlushHand()

        val expected = HandKind.Flush
        val actual = player.handKind(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handRankStraight() {
        val player = Player("", 0.0)
        player.addCard(Card(Rank.Ace, Suit.Clubs))
        player.addCard(Card(Rank.King, Suit.Spades))

        val board = listOf(Card(Rank.Queen, Suit.Spades),
                           Card(Rank.Jack, Suit.Spades),
                           Card(Rank.Ten, Suit.Spades),
                           Card(Rank.Nine, Suit.Spades),
                           Card(Rank.Eight, Suit.Spades))

        val expected = HandKind.StraightFlush
        val actual = player.handKind(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handRankThreeOfAKind() {
        val player = Player("", 0.0)
        player.addCard(Card(Rank.Ace, Suit.Clubs))
        player.addCard(Card(Rank.Ace, Suit.Spades))

        val board = listOf(Card(Rank.Ace, Suit.Diamonds),
                           Card(Rank.Jack, Suit.Spades),
                           Card(Rank.Ten, Suit.Spades),
                           Card(Rank.Nine, Suit.Spades),
                           Card(Rank.Eight, Suit.Spades))

        val expected = HandKind.StraightFlush
        val actual = player.handKind(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handRankTwoPair() {
        val (player, board) = createTwoPairHand()

        val expected = HandKind.TwoPair.rank
        val actual = player.handRank(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handKindTwoPair() {
        val (player, board) = createTwoPairHand()

        val expected = HandKind.TwoPair
        val actual = player.handKind(board)
        assertEquals(expected, actual)
    }
*/

/*
    @Test
    fun handRankPair() {
        val player = Player("", 0.0)
        player.addCard(Card(Rank.Ace, Suit.Clubs))
        player.addCard(Card(Rank.Ace, Suit.Hearts))

        val board = listOf(Card(Rank.Two, Suit.Diamonds),
                           Card(Rank.Jack, Suit.Spades),
                           Card(Rank.Ten, Suit.Spades),
                           Card(Rank.Nine, Suit.Spades),
                           Card(Rank.Eight, Suit.Spades))

        val expected = HandKind.Pair
        val actual = player.handKind(board)
        assertEquals(expected, actual)
    }
*/
