package models

import anorm.Pk

case class User (id: Pk[Long], email: String)
