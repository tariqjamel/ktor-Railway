package com.example

import com.example.model.MakeTurn
import com.example.model.TicTacToeGame
import io.ktor.server.routing.*
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json


fun Route.socket(game: TicTacToeGame) {
    route("/play") {
        webSocket {
            val player = game.connectPlayer(this)

            if (player == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "2 players already connected"))
                return@webSocket
            }

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        //Reads the text from the frame, passes it to extractAction,
                        // which parses the message and returns a MakeTurn object representing the player's move.
                        val action = extractAction(frame.readText())
                        //passing the player and the move coordinates (x and y), to update the game state.
                        game.finishedTurn(player, action.x, action.y)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                game.disconnectPlayer(player)
            }
        }
    }
}

private fun extractAction(message: String): MakeTurn {
    val type = message.substringBefore("#")
    val body = message.substringAfter("#")
    return if (type == "make_turn") {
        Json.decodeFromString(body)
    } else MakeTurn(-1, -1)
}