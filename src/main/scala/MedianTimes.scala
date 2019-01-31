import com.github.nscala_time.time.Imports._
import org.joda.time.format.DateTimeFormatter
import scala.collection.mutable
import scala.util.{Success, Try}
import com.github.tototoshi.csv._
import java.io._
import com.github.nscala_time.time.Imports
import scala.io.BufferedSource

object MedianTimes extends App {
  if (!validArguments(args)) {
    println("Invalid arguments. See readme for more info if you're unsure.")
  } else {
    val day: Option[(LocalDateTime, LocalDateTime)] = parseDate(args(1), args(2))
    val pickupData: Try[Map[String, Array[Array[String]]]] = readPickupData(args(0))
    if (pickupData.isSuccess && day.isDefined) {
      //parses the data and calculates the median pickup times for each location
      val parsedData: Try[Array[(Int, Int)]] = parsePickupData(pickupData.get, day.get)
      //writes the data to a csv if parsing is successful
      parsedData.map(writeData(_, args(0) + "/" + args(3))) match {
        case _ @ Success(Success(_)) => println("Successfully calculated median times!")
        case _ => println("Data writing failed. No data found for given time and hour or pickup data may be corrupted.")
      }
    } else {
      println("Data file missing or invalid time format given.")
    }
  }

  private def writeData(data: Array[(Int, Int)], fileName: String) =
    Try {
      val file: File = new File(s"data/$fileName")
      CSVWriter.open(file)
    }.map{ csvWriter =>
      Try {
        val formattedData: Seq[Seq[Int]] = data.map(i => Seq(i._1, i._2)).toSeq
        val header: Seq[String] = Seq("location_id", "median_pickup_time")
        csvWriter.writeAll(header +: formattedData)
      }
    }

  /* Supported date formats:
      DD-MM-YY
      DD-MM-YYYY
      YYYY-MM-DD */
  private def parseDate(dateString: String, timeString: String): Option[(Imports.LocalDateTime, Imports.LocalDateTime)] = {
    val dateData: Array[String] = dateString.split("-")
    val timeData: Array[String] = timeString.split("-")
    //sets a pattern for parsing date based on the string
    val format: Option[DateTimeFormatter] = dateData match {
      case a if a(0).length == 4 && a(1).length == 2 && a(2).length == 2 => Some(DateTimeFormat.forPattern("YYYY-MM-DD-HH"))
      case a if a(0).length == 2 && a(1).length == 2 && a(2).length == 4 => Some(DateTimeFormat.forPattern("DD-MM-YYYY-HH"))
      case a if a(0).length == 2 && a(1).length == 2 && a(2).length == 2 => Some(DateTimeFormat.forPattern("DD-MM-YY-HH"))
      case _ => None
    }
    //add leading zeroes to hours if needed
    val timeStart: String = timeData.head.reverse.padTo(2, '0').reverse
    val timeEnd: String = timeData.last.reverse.padTo(2, '0').reverse
    format.map { i =>
      val start: LocalDateTime = DateTime.parse(dateString + "-" + timeStart, i).toLocalDateTime
      val end: LocalDateTime = DateTime.parse(dateString + "-" + timeEnd, i).toLocalDateTime
      (start, end)
    }
  }

  private def validArguments(args: mutable.WrappedArray[String]): Boolean = {
    args.length == 4 &&
      args(0).forall(_.isLetter) &&
      (args(1).length == 8 || args(1).length == 10) &&
      args(1).count(_ == '-') == 2 &&
      args(1).split("-").forall(i => i.forall(_.isDigit)) &&
      args(2).count(_ == '-') == 1 &&
      args(2).split("-").forall(i => i.forall(_.isDigit)) &&
      args(2).split("-").map(_.toInt).sliding(2).forall(i => i.head < i.last) &&
      args(3).endsWith(".csv")
  }

  private def parsePickupData(pickupData: Map[String, Array[Array[String]]],
                              date: (LocalDateTime, LocalDateTime)): Try[Array[(Int, Int)]] = {
    Try {
      //filter to only include correct times
      val filtered: Map[String, Array[Array[String]]] = pickupData.map{ i =>
        (i._1, i._2.filter{ j =>
          val iterDate: LocalDateTime = DateTime.parse(j(1)).toLocalDateTime
          iterDate >= date._1 && iterDate <= date._2
        })
      }
      //get the median pickup time
      filtered.map{ i =>
        (i._1.toInt, i._2.map(_.last.toInt).sorted match {
          case pickups if pickups.length % 2 == 0 => //an even number of data
            val (first, second) = pickups.splitAt(pickups.length / 2)
            (first.last + second.head) / 2
          case pickups => //an odd number of data
            pickups(pickups.length / 2)
        })}.toArray.sortBy(_._1)
    }
  }

  private def readPickupData(city: String): Try[Map[String, Array[Array[String]]]] = {
    Try {
      val pickupTimesFile: BufferedSource = io.Source.fromFile(s"data/$city/pickup_times.csv")
      val pickupTimes: Array[Array[String]] = pickupTimesFile.getLines().drop(1).map(_.split(",")).toArray
      pickupTimes.groupBy(_.head)
    }
  }
}