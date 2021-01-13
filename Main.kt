package calculator

import java.math.BigDecimal

var variables = mutableMapOf<String, BigDecimal>()
val operatorPrecedence = mapOf(
    ")" to 0,
    "+" to 1,
    "-" to 1,
    "*" to 2,
    "/" to 2,
    "^" to 3,
    "(" to 4
)

fun main() {
    while (true) {
        val input = readLine()!!.trim()
        try {
            if (input.isNotEmpty()) {
                when {
                    isCommand(input) -> if (executeCommand(input) == 1) return
                    isAssignment(input) -> assignVariable(input)
                    isExpression(input) -> println(evaluate(input))
                    else -> throw InvalidExpressionException()
                }
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }
}

// Evaluate an expression
fun evaluate(expression: String): BigDecimal {
    val postfix = toPostfix(expression)
    val stack = mutableListOf<BigDecimal>()

    for (arg in postfix.split(" ")) {
        stack.add(if (isNumeric(arg)) arg.toBigDecimal()
        else {
            val a = stack.removeLast()
            val b = stack.removeLast()
            when (arg) {
                "+" -> b + a
                "-" -> b - a
                "*" -> b * a
                "/" -> b / a
                "^" -> if (a.toInt().toBigDecimal() == a) b.pow(a.toInt()) else throw InvalidExpressionException()
                else -> throw InvalidExpressionException()
            }
        })
    }
    return stack.removeLast()
}

// Prepare the infix expression for conversion
fun prepareInfix(input: String): String {
    var infix = input.replace(" ", "")
    if ("**" in infix || "//" in infix) throw InvalidExpressionException()

    while ("++" in infix || "--" in infix || "+-" in infix || "-+" in infix) {
        infix = infix.replace("++", "+").replace("--", "+").replace("-+", "-").replace("+-", "-")
    }
    infix = infix.replace("+", " + ").replace("-", " - ").replace("*", " * ").replace("/", " / ")
                 .replace("^", " ^ ").replace("=", " = ").replace("(", " ( ").replace(")", " ) ")
                 .replace("= +", " = ")
                 .replace("\\s+".toRegex(), " ").trim()

     return when (infix.first()) {
        '+' -> infix.removeRange(0, 2)
        '-' -> infix.replaceRange(0, 2, "-")
        else -> infix
    }
}

// Converts infix (normal) expressions to postfix or RPN (Reverses Polish Notation)
fun toPostfix(input: String): String {
    val operators = mutableListOf<String>() // Operator Queue
    val infix = prepareInfix(input)
    var postfix = ""

    for (arg in infix.split(" ")) {
        if (isNumeric(arg)) {
            postfix += " $arg"
        }
        else if (isValidIdentifier(arg)) {
            if (variables.containsKey(arg)) postfix += " ${variables[arg]}"
            else throw UnknownVariableException()
        }
        else if (isOperator(arg)) {
            if (operators.isEmpty() || operators.first() == "("
                || operatorPrecedence[arg]!! > operatorPrecedence[operators.first()]!!) {
                operators.add(0, arg)
            }
            else if (arg == ")") {
                if (operators.isEmpty() || !operators.contains("(")) throw InvalidExpressionException()
                while (operators.first() != "(") {
                    postfix += " ${operators.removeFirst()}"
                }
                operators.removeFirst()
            }
            else if (operatorPrecedence[arg]!! <= operatorPrecedence[operators.first()]!!) {
                while (operators.isNotEmpty() && (operatorPrecedence[arg]!! <= operatorPrecedence[operators.first()]!!
                            && operators.first() != "(")) {
                    postfix += " ${operators.removeFirst()}"
                }
                operators.add(0, arg)
            }
        }
        else throw InvalidExpressionException()
    }

    for (operator in operators) {
        if (operator in "()") throw InvalidExpressionException()
        postfix += " $operator"
    }
    operators.clear()

    return postfix.trim()
}


fun isCommand(input: String): Boolean = input.first() == '/'

fun isAssignment(input: String): Boolean = Regex("([\\w]*)\\s*=\\s*(.+)").matches(input)

fun isExpression(input: String): Boolean = Regex("[-+*/^()a-zA-Z0-9]").containsMatchIn(input)

fun isValidIdentifier(identifier: String): Boolean = Regex("[a-zA-Z]+").matches(identifier)

fun isNumeric(input: String): Boolean = Regex("^[-+]?\\s*\\d+(\\.\\d+)?$").matches(input)

fun isOperator(input: String): Boolean = Regex("[+/\\-*^()]").matches(input)

fun hasMultipleAssignments(input: String): Boolean = input.split("=").size > 2

// Extract variable name and value from input and add it to the map
fun assignVariable(input: String) {
    if (hasMultipleAssignments(input)) throw InvalidAssignmentException()

    val (variable, value) = input.split("=").map { it.trim() }

    if (!isValidIdentifier(variable)) throw InvalidIdentifierException()
    else if (!isNumeric(value)) {
        if (!isValidIdentifier(value)) throw InvalidAssignmentException()
        else if (variables.contains(value)) variables[variable] = variables[value]!!
        else throw UnknownVariableException()
    }
    else
        variables[variable] = value.toBigDecimal()
}

// Execute the calculator commands
fun executeCommand(input: String): Int {
    when (input) {
        "/exit" -> {
            println("Bye!")
            return 1
        }
        "/help" -> {
            println("""
                Smart Calculator Help:
                
                the calculator uses basic arithmetic operations:
                Add, Subtract, Multiply, Divide, Power
                you can use + - * / ^ for the respective operations.
                
                 e.g. 
                 Enter the following expression at the prompt,
                 and press the ENTER key to display the expression result.
                  
                 3 + 4 * 12 / (6 - 2) ^ 2
                 
                 You can also define variables and store values in them.
                 
                 e.g. 
                 a = 5
                 b = 7
                 c = a
                 
                 Enter the name of the variable and press ENTER
                 to display the value of the variable:
                 a
                 5
                
                Enter the following commands for respective actions:
                 /help      to display this help screen
                 /variables  to display defined variables
                 /exit      to exit the calculator
            """.trimIndent())
        }
        "/variables" -> for ((key, value) in variables) println("$key = $value")
        else -> throw UnknownCommandException()
    }
    return 0
}

class UnknownCommandException(message: String = "Unknown command"): Exception(message)
class UnknownVariableException(message: String = "Unknown variable"): Exception(message)
class InvalidIdentifierException(message: String = "Invalid identifier"): Exception(message)
class InvalidAssignmentException(message: String = "Invalid assignment"): Exception(message)
class InvalidExpressionException(message: String = "Invalid expression"): Exception(message)