/*
 * Copyright (C) 2018-2020  All sonar-scala contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mwz.sonar.scala
package scalastyle

import com.mwz.sonar.scala.qualityprofiles.Overrides
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.{
  NewBuiltInActiveRule,
  NewBuiltInQualityProfile
}

/**
 * Defines a Scalastyle quality profile.
 */
final class ScalastyleQualityProfile extends BuiltInQualityProfilesDefinition {
  override def define(context: BuiltInQualityProfilesDefinition.Context): Unit = {
    // Create an empty profile.
    val profile = context.createBuiltInQualityProfile(ScalastyleQualityProfile.ProfileName, Scala.LanguageKey)

    // Ensure this is not the default profile.
    profile.setDefault(false)

    // Activate all rules in the Scalastyle rules repository.
    // (except for those which were not included in the repository)
    ScalastyleQualityProfile.activateRules(profile)

    // Save the profile.
    profile.done()
  }
}

object ScalastyleQualityProfile {
  private[scalastyle] final val ProfileName: String = "Scalastyle"

  /**
   * Activates Scalastyle rules for the given profile excluding blacklisted rules.
   * Overrides the default severity and parameter values if provided in overrides.
   */
  def activateRules(profile: NewBuiltInQualityProfile, overrides: Option[Overrides] = None): Unit = {
    ScalastyleInspections.AllInspections
      .filterNot { inspection =>
        ScalastyleRulesRepository.SkipTemplateInstances.contains(inspection.clazz) ||
        ScalastyleRulesRepository.BlacklistRules.contains(inspection.clazz) ||
        overrides.exists(_.blacklist.contains(inspection.clazz))
      }
      .foreach { inspection =>
        val rule: NewBuiltInActiveRule =
          profile.activateRule(ScalastyleRulesRepository.RepositoryKey, inspection.clazz)

        overrides.foreach { overrides =>
          // Override the severity.
          overrides.severities
            .get(inspection.clazz)
            .foreach(severity => rule.overrideSeverity(severity.name))

          // Override rule params.
          overrides.params
            .get(inspection.clazz)
            .foreach {
              _.foreach {
                case (k, v) => rule.overrideParam(k, v)
              }
            }
        }
      }
  }
}
