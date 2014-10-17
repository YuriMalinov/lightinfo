package system

import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.internals.{FieldMetaData, StatementWriter}
import org.squeryl.{Session, Table}

class MyPostgreSqlAdapter extends PostgreSqlAdapter {
  // Skip length in postgres for text columns...
  override def stringTypeDeclaration(length: Int): String = length match {
    case -1 => stringTypeDeclaration
    case _ => s"character varying ($length)"
  }

  override def stringTypeDeclaration: String = "text"

  override def createSequenceName(fmd: FieldMetaData): String = fmd.parentMetaData.viewOrTable.name + "_" + fmd.columnName + "_seq"

  def makeSequenceName(t: Table[_], name: String) = t.schema.name match {
    case Some(n) => n + "." + name
    case None => name
  }

  /**
   * Override super to set column default to sequence + to handle schema name
   */
  override def postCreateTable(t: Table[_], printSinkWhenWriteOnlyMode: Option[String => Unit]) = {
    val autoIncrementedFields = t.posoMetaData.fieldsMetaData.filter(_.isAutoIncremented)

    for (fmd <- autoIncrementedFields) {
      val seq = new StatementWriter(false, this)
      val sequenceName = quoteName(makeSequenceName(t, fmd.sequenceName))
      seq.write("create sequence ", sequenceName)
      val default = new StatementWriter(false, this)
      default.write("alter table ", quoteName(t.prefixedName), " alter column ", quoteName(fmd.columnName), " set default nextval('", sequenceName, "')")

      if (printSinkWhenWriteOnlyMode == None) {
        val st = Session.currentSession.connection.createStatement
        st.execute(seq.statement)
        st.execute(default.statement)
      } else {
        printSinkWhenWriteOnlyMode.get.apply(seq.statement + ";")
        printSinkWhenWriteOnlyMode.get.apply(default.statement + ";")
      }
    }
  }

  override def writeInsert[T](o: T, t: Table[T], sw: StatementWriter):Unit = {

    val o_ = o.asInstanceOf[AnyRef]

    val autoIncPK = t.posoMetaData.fieldsMetaData.find(fmd => fmd.isAutoIncremented)

    if(autoIncPK == None) {
      super.writeInsert(o, t, sw)
      return
    }

    val f = getInsertableFields(t.posoMetaData.fieldsMetaData)

    val colNames = List(autoIncPK.get) ::: f.toList
    val colVals = List("nextval('" + quoteName(makeSequenceName(t, autoIncPK.get.sequenceName)) + "')") ::: f.map(fmd => writeValue(o_, fmd, sw)).toList

    sw.write("insert into ")
    sw.write(quoteName(t.prefixedName))
    sw.write(" (")
    sw.write(colNames.map(fmd => quoteName(fmd.columnName)).mkString(", "))
    sw.write(") values ")
    sw.write(colVals.mkString("(",",",")"))
  }
}
