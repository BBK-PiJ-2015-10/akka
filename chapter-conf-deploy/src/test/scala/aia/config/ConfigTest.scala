package aia.config

import akka.actor.ActorSystem
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.must.Matchers
import com.typesafe.config.{ConfigException, ConfigFactory}


class ConfigTest extends AnyWordSpecLike with Matchers {

    "Configuration" must {

      "has configuration" in  {

        val mySystem = ActorSystem("myMickey")
        val config = mySystem.settings.config
        config.getInt("myTest.intParam") must be (20)
        config.getString("myTest.applicationDesc") must be("My Config Test")
      }

      "has defaults" in {

        val mySystem = ActorSystem("myMouse")
        val config = mySystem.settings.config
        config.getInt("myTestDefaults.intParam") must be (15)
        config.getString("myTestDefaults.applicationDesc") must be ("My Current Test")

      }

      "can include file" in {

        val mySystem = ActorSystem("myDog")
        val config = mySystem.settings.config
        config.getInt("myTestIncluded.intParam") must be (10)
        config.getString("myTestIncluded.applicationDesc") must be ("My Included Test")

      }


      "can load file" in {

        val configuration = ConfigFactory.load("mickey")
        val mySystem = ActorSystem("myOtherOne",configuration)
        val config = mySystem.settings.config
        config.getInt("myTestLoad.intParam") must be (30)
        config.getString("myTestLoad.applicationDesc") must be ("My Load Test")

      }

      "can lifted" in {

        val configuration = ConfigFactory.load("lift")
        val mySystem =  ActorSystem("myFirstLiftTest",configuration.getConfig("myTestLift").withFallback(configuration))
        val config = mySystem.settings.config
        config.getInt("myTest.intParam") must be (40)
        config.getString("myTest.applicationDesc") must be ("Game Over Sucker")
        config.getString("myTestLift.rootParam") must be ("root")
        config.getString("rootParam") must be ("root")


        val mySystem2 = ActorSystem("mySecondLiftTest",configuration.getConfig("myTestLift").withOnlyPath("myTest").withFallback(configuration))
        val config2 =  mySystem2.settings.config
        config2.getInt("myTest.intParam") must be (40)
        config2.getString("myTest.applicationDesc") must be ("Game Over Sucker")
        //evaluating { config2.getString("rootParam")} must produce [ConfigException.Missing]
        config2.getString("myTestLift.rootParam") must be ("root")


      }

    }



}
