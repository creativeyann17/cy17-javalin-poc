package com.creativeyann17.javalinpoc;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
  final Javalin app = new App().app; // inject any dependencies you might have
 // private final String usersJson = new JavalinJackson().toJsonString(UserController.users);

  @Test
  public void integrationTest() {
    JavalinTest.test(app, (server, client) -> {
      assertEquals(200, client.get("/bench").code());
      assertEquals("<h1>foo</h1>", client.get("/render").body().string());
    });
  }

}