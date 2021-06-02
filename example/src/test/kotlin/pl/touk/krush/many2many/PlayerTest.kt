package pl.touk.krush.many2many

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import pl.touk.krush.base.BaseDatabaseTest

class PlayerTest : BaseDatabaseTest() {

    @Test
    fun shouldHandleManyToManyWithEmbeddedId() {
        transaction {
            // given
            val colt = Character(id = CharacterId(1, 2), name = "Colt")
            val rico = Character(id = CharacterId(2, 3), name = "Rico")
            val spike = Character(id = CharacterId(3, 1), name = "Spike")
            val (savedColt, savedRico, savedSpike) = listOf(colt, rico, spike).map(CharacterTable::insert)

            val player1 = Player(name = "Alpha", characters = listOf(savedColt, savedRico))
            val player2 = Player(name = "Bravo", characters = listOf(savedRico, savedSpike))

            val (_, savedPlayer2) = listOf(player1, player2).map(PlayerTable::insert)

            // when
            val selectedPlayers = (PlayerTable leftJoin PlayerCharactersTable leftJoin CharacterTable)
                .select { PlayerTable.name eq "Bravo" }
                .toPlayerList()

            // then
            assertThat(selectedPlayers).containsExactly(savedPlayer2)
        }
    }

}
