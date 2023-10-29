import javalib.impworld.World;
import javalib.impworld.WorldScene;

import javalib.worldimages.*;
import tester.*;
import java.awt.*;
import java.util.*;

// represents a cell
class Cell {
  int x;
  int y;

  ArrayList<Edge> edges = new ArrayList<Edge>();

  Cell top;
  Cell right;

  Cell bottom;
  Cell left;

  Cell last;

  boolean isRight;
  boolean isBottom;

  boolean seen;

  Cell(int x, int y) {
    this.x = x;
    this.y = y;

    this.top = null;
    this.right = null;
    this.bottom = null;
    this.left = null;

    this.last = null;

    this.isRight = true;
    this.isBottom = true;

    this.seen = false;
  }

  // Draws the right wall of a cell
  WorldImage drawRight() {
    return new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
  }

  // Draws the bottom wall of a cell
  WorldImage drawBottom() {
    return new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
  }

  // Draws the cells
  WorldImage draw(int x, int y, Color color) {
    return new RectangleImage(20, 20, OutlineMode.SOLID, color).movePinhole(-11, -11);
  }

}

//represents a user
class User {
  Cell first;

  User(Cell first) {
    this.first = first;
  }

  // returns whether you can move the game controls
  public boolean mover(String word) {
    if (this.first.top != null && word.equals("up")) {
      return !this.first.top.isBottom;
    }

    else if (this.first.right != null && word.equals("right")) {
      return !this.first.isRight;
    }

    else if (this.first.bottom != null && word.equals("down")) {
      return !this.first.isBottom;
    }

    else if (this.first.left != null && word.equals("left")) {
      return !this.first.left.isRight;
    }

    else {
      return false;
    }
  }

  // draws the player's game icon
  WorldImage drawUser() {
    return new RectangleImage(20, 20, OutlineMode.SOLID, Color.magenta).movePinhole(-11, -11);
  }
}

// represents an edge
class Edge {
  Cell last;
  Cell next;
  int weight;

  Edge(Cell last, Cell next, int weight) {
    this.last = last;
    this.next = next;
    this.weight = weight;
  }
}

// represents the edges' costs
class Cost implements Comparator<Edge> {

  //An empty constructor for cost
  Cost() {
  }

  // Compares edges by cost
  public int compare(Edge first, Edge second) {
    return first.weight - second.weight;
  }
}

//represents the world
class MazeWorld extends World {
  int x;
  int y;

  Cell lastCell;

  ArrayList<Edge> edgeList1 = new ArrayList<Edge>();
  ArrayList<Edge> edgeList2 = new ArrayList<Edge>();

  HashMap<Cell, Cell> map = new HashMap<Cell, Cell>();

  ArrayList<ArrayList<Cell>> board;

  ArrayList<Cell> c = new ArrayList<Cell>();

  boolean finish;

  User one;

  WorldScene scene = new WorldScene(0, 0);

  double clock;
  TextImage clockPic;
  
  WorldImage blank = new RectangleImage(1000, 1000, OutlineMode.SOLID, Color.pink);
  TextImage win = new TextImage("Congrats, you won :)", 18, Color.BLACK);
  TextImage lose = new TextImage("Sorry, you lost :(", 18, Color.BLACK);

  // constructor for the maze
  MazeWorld(int x, int y) {
    this.x = x;
    this.y = y;
    this.board = this.makeBoard(x, y);

    this.edgeList(this.board);
    this.map(this.board);
    this.kruskal();

    this.one = new User(board.get(0).get(0));
    this.lastCell = this.board.get(this.y - 1).get(this.x - 1);
    this.finish = false;
    this.clock = 0.5 * this.x * this.y;

    this.clockPic = new TextImage("Time remaining: " + this.clock, 15, Color.blue);

    this.drawBoard();
  }

  // constructor for testing purposes
  MazeWorld() {
    this.x = 2;
    this.y = 2;
    this.board = this.makeBoard(2, 2);
    this.board.get(0).get(0).isRight = false;
    this.board.get(0).get(1).isRight = true;
    this.board.get(1).get(0).isRight = true;
    this.board.get(1).get(1).isRight = true;

    this.map.put(this.board.get(0).get(0), this.board.get(0).get(0));
    this.map.put(this.board.get(0).get(1), this.board.get(0).get(1));
    this.map.put(this.board.get(1).get(0), this.board.get(1).get(0));
    this.map.put(this.board.get(1).get(1), this.board.get(1).get(1));

    this.board.get(0).get(0).isBottom = false;
    this.board.get(0).get(1).isBottom = false;
    this.board.get(1).get(0).isBottom = true;
    this.board.get(1).get(1).isBottom = true;

    this.edgeList1 = new ArrayList<Edge>(Arrays.asList(new Edge(new Cell(0, 0), new Cell(1, 1), 1),
        new Edge(new Cell(0, 0), new Cell(0, 0), 2), new Edge(new Cell(0, 1), new Cell(1, 2), 3),
        new Edge(new Cell(1, 0), new Cell(2, 1), 3), new Edge(new Cell(1, 1), new Cell(0, 4), 8)));

    this.edgeList2 = new ArrayList<Edge>(Arrays.asList(new Edge(new Cell(0, 0), new Cell(1, 1), 1),
        new Edge(new Cell(0, 0), new Cell(0, 0), 2), new Edge(new Cell(0, 1), new Cell(1, 1), 4),
        new Edge(new Cell(1, 0), new Cell(0, 2), 87),
        new Edge(new Cell(1, 1), new Cell(1, 2), 12)));

    this.one = new User(this.board.get(0).get(0));
    this.finish = false;
    this.lastCell = this.board.get(1).get(1);
    this.clock = 0.5 * this.x * this.y;
    this.c = new ArrayList<Cell>();

    this.clockPic = new TextImage("Time remaining: " + this.clock, 15, Color.blue);

    this.scene.placeImageXY(clockPic, 30, 30);

    this.drawBoard();
  }

  // sets up the board
  ArrayList<ArrayList<Cell>> makeBoard(int x, int y) {
    ArrayList<ArrayList<Cell>> board = new ArrayList<ArrayList<Cell>>();
    for (int i = 0; i < y; i++) {
      board.add(new ArrayList<Cell>());
      ArrayList<Cell> r = board.get(i);
      for (int j = 0; j < x; j++) {
        r.add(new Cell(j, i));
      }
    }
    this.connectCells(board);
    this.edgeList(board);
    this.map(board);
    return board;
  }

  //Represents the world's scene to the user
  public WorldScene makeScene() {

    if (!c.isEmpty()) {

      Cell update = c.remove(0);

      if (c.size() > 0) {

        this.scene.placeImageXY(update.draw(this.x, this.y, Color.red), this.x * 17, this.y * 17);
      }

      else {
        this.scene.placeImageXY(update.draw(this.x, this.y, Color.pink), this.x * 17, this.y * 17);

        if (!this.lastCell.left.isRight && this.lastCell.left.last != null) {
          
          this.lastCell.last = this.lastCell.left;
        }

        else if (!this.lastCell.top.isBottom && this.lastCell.top.last != null) {
          this.lastCell.last = this.lastCell.top;
        }

        else {
          this.lastCell.last = update;
        }
        
        this.finish = true;
      }
    }

    else if (this.lastCell.last != null && this.finish) {

      if (this.lastCell.x == this.x - 1 && this.lastCell.y == this.y - 1) {
        
        this.scene.placeImageXY(this.lastCell.draw(this.x, this.y, Color.green),
            20 * this.lastCell.x, 20 * this.lastCell.y);
      }

      this.scene.placeImageXY(this.lastCell.last.draw(this.x, this.y, Color.green),
          20 * this.lastCell.last.x, 20 * this.lastCell.last.y);
      
      this.lastCell = this.lastCell.last;
    }

    if (one.first == this.board.get(this.y - 1).get(this.x - 1)) {
      
      this.scene.placeImageXY(blank, this.x, this.y);
      this.scene.placeImageXY(win, 10 * this.x, 10 * this.y);

      this.clock = 0;
    }

    else if (this.clock <= 0) {
      this.scene.placeImageXY(blank, this.x, this.y);
      this.scene.placeImageXY(lose, this.x * 10, this.y * 10);

      this.clock = 0;
    }

    else if (this.one.first != this.board.get(this.y - 1).get(this.x - 1)
        && this.one.first != this.lastCell) {

      this.clock -= 0.007;

      this.clockPic.text = "Time remaining: " + (int) this.clock;

    }

    return scene;

  }

  // Represents the world's starting points to the user
  public WorldScene drawBoard() {
    this.scene.placeImageXY(board.get(0).get(0).draw(this.x, this.y, Color.yellow), 0, 0);

    this.scene.placeImageXY(board.get(this.y - 1).get(this.x - 1).draw(this.x, this.y, Color.cyan),
        (this.x - 1) * 20, (this.y - 1) * 20);

    for (int i = 0; i < y; i++) {
      for (int j = 0; j < x; j++) {

        this.updateBottom(this.board.get(i).get(j));
        this.updateRight(this.board.get(i).get(j));

        if (this.board.get(i).get(j).seen) {
          this.scene.placeImageXY(board.get(i).get(j).draw(20, 20, Color.pink), j * 20, i * 20);
        }

        if (board.get(i).get(j).isRight) {
          this.scene.placeImageXY(board.get(i).get(j).drawRight(), (j * 20), (i * 20));
        }

        if (board.get(i).get(j).isBottom) {
          this.scene.placeImageXY(board.get(i).get(j).drawBottom(), (j * 20), (i * 20));
        }
      }

    }

    this.scene.placeImageXY(one.drawUser(), this.one.first.x * 20, this.one.first.y * 20);
    this.scene.placeImageXY(this.clockPic, 100, 220);

    return scene;
  }

  // EFFECT: restarts the board again from the beginning
  // Resets the game to start again which the user press the r key
  public void onKeyReleased(String k) {
    if (one.mover("up") && k.equals("up")) {

      this.one.first.seen = true;
      this.one.first = this.one.first.top;

    }

    else if (this.one.mover("right") && k.equals("right")) {

      this.one.first.seen = true;
      this.one.first = this.one.first.right;

    }

    else if (this.one.mover("down") && k.equals("down")) {

      this.one.first.seen = true;
      this.one.first = this.one.first.bottom;

    }

    else if (this.one.mover("left") && k.equals("left")) {

      this.one.first.seen = true;
      this.one.first = this.one.first.left;

    }

    else if (k.equals("r")) {

      this.scene = this.getEmptyScene();
      this.board = makeBoard(this.x, this.y);
      this.edgeList(this.board);
      this.map(this.board);
      this.kruskal();

      this.one = new User(this.board.get(0).get(0));
      this.clock = 0.5 * this.x * this.y;
      this.lastCell = this.board.get(this.y - 1).get(this.x - 1);

      this.drawBoard();
    }

    else if (k.equals("b")) {

      this.lastCell = this.board.get(this.y - 1).get(this.x - 1);

      this.c = this.searchPath(this.board.get(0).get(0), this.board.get(this.y - 1).get(this.x - 1),
          new Queue<>());
    }

    else if (k.equals("d")) {

      this.lastCell = this.board.get(this.y - 1).get(this.x - 1);

      this.c = this.searchPath(this.board.get(0).get(0), this.board.get(this.y - 1).get(this.x - 1),
          new Stack<>());
    }

    this.scene.placeImageXY(this.one.drawUser(), this.one.first.x * 20, this.one.first.y * 20);
    this.drawBoard();
  }

  // this method implements kruskal's algorithm
  ArrayList<Edge> kruskal() {

    for (int i = 0; i < this.edgeList1.size()
        && this.edgeList2.size() < this.edgeList1.size(); i++) {

      Edge e = edgeList1.get(i);
      if (!(this.correctCell(this.correctCell(e.last))
          .equals(this.correctCell(this.correctCell(e.next))))) {
        edgeList2.add(e);
        merge(this.correctCell(e.last), this.correctCell(e.next));
      }

    }

    for (int y = 0; y < this.y; y += 1) {
      for (int x = 0; x < this.x; x += 1) {
        for (Edge e : this.edgeList2) {
          if (this.board.get(y).get(x).equals(e.last) || this.board.get(y).get(x).equals(e.next)) {
            this.board.get(y).get(x).edges.add(e);
          }
        }
      }
    }
    return this.edgeList2;
  }

  // Checks if the right wall should be made for the given cell
  // EFFECT: Changes the isRight field of the cell
  void updateRight(Cell cell) {
    for (Edge e : this.edgeList2) {
      if (e.last.y == e.next.y) {
        e.last.isRight = false;
      }
    }
  }

  // Checks if the bottom wall should be made for the given cell
  // EFFECT: Changes the isBottom field of the cell
  void updateBottom(Cell cell) {
    for (Edge e : this.edgeList2) {
      if (e.last.x == e.next.x) {
        e.last.isBottom = false;
      }
    }
  }

  // connects cells next each other
  // EFFECT: Changes each cell's position
  void connectCells(ArrayList<ArrayList<Cell>> gameBoard) {
    for (int i = 0; i <= this.y - 1; i++) {
      for (int j = 0; j <= this.x - 1; j++) {
        if (i - 1 >= 0) {
          gameBoard.get(i).get(j).top = gameBoard.get(i - 1).get(j);
        }
        if (j + 1 <= this.x - 1) {
          gameBoard.get(i).get(j).right = gameBoard.get(i).get(j + 1);
        }
        if (i + 1 <= this.y - 1) {
          gameBoard.get(i).get(j).bottom = gameBoard.get(i + 1).get(j);
        }
        if (j - 1 >= 0) {
          gameBoard.get(i).get(j).left = gameBoard.get(i).get(j - 1);
        }
      }
    }
  }

  // creates an arraylist of edges
  ArrayList<Edge> edgeList(ArrayList<ArrayList<Cell>> arr) {
    Random rand = new Random();
    for (int i = 0; i < arr.size(); i++) {
      for (int j = 0; j < arr.get(i).size(); j++) {
        if (j < arr.get(i).size() - 1) {
          edgeList1.add(new Edge(arr.get(i).get(j), arr.get(i).get(j).right, rand.nextInt(20)));
        }

        if (i < arr.size() - 1) {
          edgeList1
              .add(new Edge(arr.get(i).get(j), arr.get(i).get(j).bottom, (int) rand.nextInt(20)));
        }
      }
    }
    edgeList1.sort(new Cost());
    return edgeList1;
  }

  // creates a new hashMap
  HashMap<Cell, Cell> map(ArrayList<ArrayList<Cell>> cell) {
    for (int i = 0; i < cell.size(); i++) {
      for (int j = 0; j < cell.get(i).size(); j++) {
        this.map.put(cell.get(i).get(j), cell.get(i).get(j));
      }
    }
    return map;
  }

  // Unifies two cells
  // EFFECT: Changes the hashmap's value
  void merge(Cell first, Cell second) {
    this.map.put(this.correctCell(first), this.correctCell(second));
  }

  // Finds the cell given
  Cell correctCell(Cell cell) {
    if (cell.equals(this.map.get(cell))) {
      return cell;
    }

    else {
      return this.correctCell(this.map.get(cell));
    }
  }

  //searches for a path from the beginning cell to the last cell
  ArrayList<Cell> searchPath(Cell start, Cell end, IContainer<Cell> worklist) {
    HashMap<String, Edge> cameFromEdge = new HashMap<>();
    ArrayList<Cell> path = new ArrayList<>();

    worklist.add(start);
    while (worklist.size() > 0) {
      Cell next = worklist.remove();

      if (next == end) {
        return this.reconstructPath(cameFromEdge, end);
      }
      else if (path.contains(next)) {
        // Do nothing
      }
      else {
        for (Edge e : next.edges) {
          worklist.add(e.last);
          worklist.add(e.next);
          if (path.contains(e.last)) {
            next.last = e.last;
          }
          else if (path.contains(e.next)) {
            next.last = e.next;
          }
        }
        path.add(next);
      }
    }
    return path;
  }

  // returns the path from the beginning cell to the last cell
  ArrayList<Cell> reconstructPath(HashMap<String, Edge> cameFromEdge, Cell end) {
    ArrayList<Cell> path = new ArrayList<>();
    Cell current = end;

    while (cameFromEdge.containsKey(current)) {
      Edge e = cameFromEdge.get(current);
      path.add(0, current); // Add to the beginning of the list
      if (current == e.next) {
        current = e.last;
      }
      else {
        current = e.next;
      }
    }
    path.add(0, current); // Add the start node to the beginning of the list
    return path;
  }

}


// represents a Deque
class Deque<T> {
  Sentinel<T> header;

  // constructor that takes in zero arguments
  Deque() {
    this.header = new Sentinel<T>();

  }

  // constructor that takes in one argument
  Deque(Sentinel<T> header) {
    this.header = header;

  }

  // counts the number of nodes in a list Deque, not included the header node
  int size() {
    return this.header.next.size();
  }

  // consumes a value of type T and inserts it at the front of the list
  void addAtHead(T t) {
    new Node<T>(t, this.header.next, this.header);

  }

  // consumes a value of type T and inserts it at the end of the list
  void addAtTail(T t) {
    new Node<T>(t, this.header, this.header.prev);

  }

  // removes the first node from this Deque
  T removeFromHead() {
    return this.header.next.removeHelper();

  }

  // removes the first node from this Deque
  T removeFromTail() {
    return this.header.prev.removeHelper();

  }

}

// represents a Sentinel
class Sentinel<T> extends ANode<T> {

  Sentinel() {
    super(null, null);
    this.next = this;
    this.prev = this;

  }

  // count the number of nodes in this Sentinel
  int size() {
    return 0;

  }

  // removes the node from this Sentinel
  T removeHelper() {
    throw new RuntimeException("Removing from Sentinel!");

  }

  // connects two nodes together and sets them equal to each other
  void connect(ANode<T> one, ANode<T> two) {
    two.prev = one;
    one.next = two;
  }

}

// represents the ANode
abstract class ANode<T> {
  ANode<T> next;
  ANode<T> prev;

  //constructor for ANode
  ANode(ANode<T> next, ANode<T> prev) {
    this.next = next;
    this.prev = prev;
  }

  // counts the number of Nodes in this ANode
  abstract int size();

  // removes the node from this ANode
  abstract T removeHelper();

}

// represents a Node
class Node<T> extends ANode<T> {
  T data;

  // constructor that takes in one argument
  Node(T data) {
    super(null, null);
    this.data = data;

  }

  // constructor that takes in three arguments
  Node(T data, ANode<T> next, ANode<T> prev) {
    super(next, prev);
    this.data = data;

    if (next == null || prev == null) {
      throw new IllegalArgumentException("Null Node");

    }
    
    else {
      next.prev = this;
      prev.next = this;

    }

  }

  // counts the number of nodes in this Node
  int size() {
    return 1 + this.next.size();

  }

  // remove the node from this Node
  T removeHelper() {
    prev.next = this.next;
    next.prev = this.prev;
    return this.data;

  }

}

// represents an interface for Queue and Stack
interface IContainer<T> {

  // adds an item to the container
  void add(T item);

  // removes an item from the container
  T remove();

  // returns the size of the container
  int size();
}

// represents a queue for BFS
class Queue<T> implements IContainer<T> {
  Deque<T> data;

  // empty constructor
  Queue() {
    this.data = new Deque<T>();
  }

  // constructor that takes in data
  Queue(Deque<T> data) {
    this.data = data;
  }

  // adds to this queue
  public void add(T t) {
    this.data.addAtTail(t);
  }

  // removes from this queue
  public T remove() {
    return this.data.removeFromHead();
  }

  //returns the size of this queue
  public int size() {
    return this.data.size();
  }

}

// represents a stack for DFS
class Stack<T> implements IContainer<T> {
  Deque<T> data;

  // empty constructor
  Stack() {
    this.data = new Deque<T>();
  }

  // constructor that takes in data
  Stack(Deque<T> data) {
    this.data = data;
  }

  // adds to this stack
  public void add(T t) {
    this.data.addAtHead(t);
  }

  // removes from this stack
  public T remove() {
    return this.data.removeFromHead();
  }

  // returns the size of this stack
  public int size() {
    return this.data.size();
  }
}

// Examples and tests for the maze game
class ExamplesMaze {
  MazeWorld maze = new MazeWorld(10, 10);

  ArrayList<Edge> edgeList1 = new ArrayList<Edge>(Arrays.asList(
      new Edge(new Cell(0, 0), new Cell(1, 1), 1), new Edge(new Cell(0, 0), new Cell(0, 0), 2),
      new Edge(new Cell(0, 1), new Cell(1, 2), 3), new Edge(new Cell(1, 0), new Cell(2, 1), 3),
      new Edge(new Cell(1, 1), new Cell(0, 4), 8)));

  ArrayList<Edge> edgeList2 = new ArrayList<Edge>(Arrays.asList(
      new Edge(new Cell(0, 0), new Cell(1, 1), 1), new Edge(new Cell(0, 0), new Cell(0, 0), 2),
      new Edge(new Cell(0, 1), new Cell(1, 1), 4), new Edge(new Cell(1, 0), new Cell(0, 2), 87),
      new Edge(new Cell(1, 1), new Cell(1, 2), 12)));

  Cell c1 = new Cell(5, 5);
  Cell c2 = new Cell(0, 0);
  Cell c3 = new Cell(2, 3);
  Cell c4 = new Cell(7, 2);

  User p1 = new User(c1);
  User p2 = new User(c2);
  User p3 = new User(c3);
  User p4 = new User(c4);

  Cost cost1 = new Cost();

  Edge e1 = new Edge(c1, c2, 15);
  Edge e2 = new Edge(c2, c3, 20);
  Edge e3 = new Edge(c1, c3, 10);

  Deque<String> emptyDeque;
  Deque<String> deque1;
  Deque<String> deque2;
  Deque<String> dequeTest1;
  Deque<String> dequeTest2;

  Sentinel<String> emptySentinel;
  Sentinel<String> sentinel1;
  Sentinel<String> sentinel2;
  Sentinel<String> sentinelTest1;
  Sentinel<String> sentinelTest2;

  ANode<String> nodeABC;
  ANode<String> nodeBCD;
  ANode<String> nodeCDE;
  ANode<String> nodeDEF;

  ANode<String> nodeCar;
  ANode<String> nodeBike;
  ANode<String> nodeTruck;
  ANode<String> nodeBus;
  ANode<String> nodePlane;
  ANode<String> nodeBoat;
  ANode<String> nodeRocket;

  void initDeque() {
    emptySentinel = new Sentinel<String>();
    sentinel1 = new Sentinel<String>();
    sentinel2 = new Sentinel<String>();

    emptyDeque = new Deque<String>();
    deque1 = new Deque<String>(this.sentinel1);
    deque2 = new Deque<String>(this.sentinel2);

    nodeABC = new Node<String>("abc", sentinel1, sentinel1);
    nodeBCD = new Node<String>("bcd", sentinel1, nodeABC);
    nodeCDE = new Node<String>("cde", sentinel1, nodeBCD);
    nodeDEF = new Node<String>("def", sentinel1, nodeCDE);

    nodeCar = new Node<String>("car", sentinel2, sentinel2);
    nodeBike = new Node<String>("bike", sentinel2, nodeCar);
    nodeTruck = new Node<String>("truck", sentinel2, nodeBike);
    nodeBus = new Node<String>("bus", sentinel2, nodeTruck);
    nodePlane = new Node<String>("plane", sentinel2, nodeBus);
    nodeBoat = new Node<String>("boat", sentinel2, nodePlane);
    nodeRocket = new Node<String>("rocket", sentinel2, nodeBoat);

    sentinelTest1 = new Sentinel<String>();
    dequeTest1 = new Deque<String>(this.sentinelTest1);
    sentinelTest2 = new Sentinel<String>();
    dequeTest2 = new Deque<String>(this.sentinelTest2);

  }

  // tests for drawRight()
  void testDrawRight(Tester t) {
    t.checkExpect(this.c1.drawRight(),
        new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10));

    t.checkExpect(this.c2.drawRight(),
        new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10));

    t.checkExpect(this.c3.drawRight(),
        new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10));
  }

  // tests for drawBottom()
  void testDrawBottom(Tester t) {
    t.checkExpect(this.c1.drawBottom(),
        new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20));

    t.checkExpect(this.c2.drawBottom(),
        new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20));

    t.checkExpect(this.c3.drawBottom(),
        new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20));
  }

  // tests for draw()
  void testDraw(Tester t) {
    t.checkExpect(this.c1.draw(5, 5, Color.gray),
        new RectangleImage(20, 20, OutlineMode.SOLID, Color.gray).movePinhole(-11, -11));

    t.checkExpect(this.c2.draw(0, 0, Color.pink),
        new RectangleImage(20, 20, OutlineMode.SOLID, Color.pink).movePinhole(-11, -11));

    t.checkExpect(this.c3.draw(2, 3, Color.orange),
        new RectangleImage(20, 20, OutlineMode.SOLID, Color.orange).movePinhole(-11, -11));
  }

  // tests for drawUser()
  void testDrawUser(Tester t) {
    t.checkExpect(this.p1.drawUser(),
        new RectangleImage(20, 20, OutlineMode.SOLID, Color.magenta).movePinhole(-11, -11));

    t.checkExpect(this.p2.drawUser(),
        new RectangleImage(20, 20, OutlineMode.SOLID, Color.magenta).movePinhole(-11, -11));

    t.checkExpect(this.p3.drawUser(),
        new RectangleImage(20, 20, OutlineMode.SOLID, Color.magenta).movePinhole(-11, -11));
  }

  // tests for compare()
  void testCompare(Tester t) {
    t.checkExpect(this.cost1.compare(e1, e2), -5);

    t.checkExpect(this.cost1.compare(e2, e3), 10);

    t.checkExpect(this.cost1.compare(e3, e3), 0);
  }

  // Test for makeBoard()
  void testMakeBoard(Tester t) {
    MazeWorld maze2 = new MazeWorld();

    t.checkExpect(maze2.board, new ArrayList<ArrayList<Cell>>(Arrays.asList(
        new ArrayList<Cell>(Arrays.asList(maze2.board.get(0).get(0), maze2.board.get(0).get(1))),

        new ArrayList<Cell>(Arrays.asList(maze2.board.get(1).get(0), maze2.board.get(1).get(1))))));
  }

  // Tests for connectCells()
  void testConnectCells(Tester t) {
    MazeWorld maze2 = new MazeWorld();

    t.checkExpect(maze2.board.get(0).get(0).top, null);
    t.checkExpect(maze2.board.get(0).get(0).left, null);
    t.checkExpect(maze2.board.get(0).get(0).right, maze2.board.get(0).get(1));
    t.checkExpect(maze2.board.get(0).get(0).bottom, maze2.board.get(1).get(0));
  }

  // Tests for edgeList()
  void testMakeEdges(Tester t) {
    MazeWorld maze2 = new MazeWorld();

    t.checkExpect(maze2.edgeList1.get(0),
        new Edge(new Cell(maze2.board.get(0).get(0).x, maze2.board.get(0).get(0).y),
            new Cell(maze2.board.get(0).get(1).x, maze2.board.get(1).get(1).y), 1));

    t.checkExpect(maze2.edgeList1.get(1),
        new Edge(new Cell(maze2.board.get(0).get(0).x, maze2.board.get(0).get(0).y),
            new Cell(maze2.board.get(1).get(0).x, maze2.board.get(0).get(0).y), 2));

  }

  // Tests for map()
  void testMap(Tester t) {
    MazeWorld maze2 = new MazeWorld();
    t.checkExpect(maze2.map.get(maze2.board.get(0).get(0)), maze2.board.get(0).get(0));
    t.checkExpect(maze2.map.get(maze2.board.get(0).get(1)), maze2.board.get(0).get(1));
    t.checkExpect(maze2.map.get(maze2.board.get(1).get(0)), maze2.board.get(1).get(0));
    t.checkExpect(maze2.map.get(maze2.board.get(1).get(1)), maze2.board.get(1).get(1));
  }

  // Tests for kruskal()
  void testKruskal(Tester t) {
    MazeWorld maze2 = new MazeWorld();

    maze2.makeBoard(maze2.x, maze2.x);
    t.checkExpect(maze2.edgeList2.get(0),
        new Edge(maze2.edgeList2.get(0).last, maze2.edgeList2.get(0).next, 1));

    t.checkExpect(maze2.edgeList2.get(1),
        new Edge(maze2.edgeList2.get(1).last, maze2.edgeList2.get(1).next, 2));

    t.checkExpect(maze2.edgeList2.get(2),
        new Edge(maze2.edgeList2.get(2).last, maze2.edgeList2.get(2).next, 4));

    t.checkExpect(maze2.edgeList2.get(3),
        new Edge(maze2.edgeList2.get(3).last, maze2.edgeList2.get(3).next, 87));

    t.checkExpect(maze2.edgeList2.get(4),
        new Edge(maze2.edgeList2.get(4).last, maze2.edgeList2.get(4).next, 12));

  }

  // Tests for merge()
  void testMerge(Tester t) {
    MazeWorld maze2 = new MazeWorld();

    maze2.merge(maze2.board.get(0).get(0), maze2.board.get(0).get(1));
    t.checkExpect(maze2.correctCell(maze2.board.get(0).get(0)), maze2.board.get(0).get(1));

    maze2.merge(maze2.board.get(0).get(1), maze2.board.get(1).get(1));
    t.checkExpect(maze2.correctCell(maze2.board.get(0).get(1)), maze2.board.get(1).get(1));

    maze2.merge(maze2.board.get(1).get(0), maze2.board.get(0).get(1));
    t.checkExpect(maze2.correctCell(maze2.board.get(0).get(0)), maze2.board.get(1).get(1));
  }

  // Tests for correctCell()
  void testCorrectCell(Tester t) {
    MazeWorld maze2 = new MazeWorld();
    t.checkExpect(maze2.correctCell(maze2.board.get(0).get(0)), maze2.board.get(0).get(0));
    t.checkExpect(maze2.correctCell(maze2.board.get(1).get(0)), maze2.board.get(1).get(0));
  }

  // Tests for updateRight()
  void testUpdateRight(Tester t) {
    MazeWorld maze2 = new MazeWorld();

    maze2.updateRight(maze2.board.get(0).get(0));
    t.checkExpect(maze2.board.get(0).get(0).isRight, false);

    maze2.updateRight(maze2.board.get(1).get(0));
    t.checkExpect(maze2.board.get(1).get(0).isRight, true);
  }

  // Tests for updateBottom()
  void testUpdateBottom(Tester t) {
    MazeWorld maze2 = new MazeWorld();
    maze2.updateBottom(maze2.board.get(0).get(0));
    t.checkExpect(maze2.board.get(0).get(0).isBottom, false);

    maze2.updateBottom(maze2.board.get(0).get(1));
    t.checkExpect(maze2.board.get(0).get(1).isBottom, false);

    maze2.updateBottom(maze2.board.get(1).get(0));
    t.checkExpect(maze2.board.get(1).get(0).isBottom, true);

    maze2.updateBottom(maze2.board.get(1).get(1));
    t.checkExpect(maze2.board.get(1).get(1).isBottom, true);

  }

  // test for drawBoard()
  void testDrawBoard(Tester t) {
    MazeWorld maze2 = new MazeWorld();

    WorldScene background = new WorldScene(0, 0);

    background.placeImageXY(new RectangleImage(0, 0, OutlineMode.OUTLINE, Color.black), 0, 0);

    WorldImage r1 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.pink).movePinhole(-10, -10);
    background.placeImageXY(r1, 0, 0);

    WorldImage r2 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.cyan).movePinhole(-10, -10);
    background.placeImageXY(r2, 20, 20);

    WorldImage r3 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r3, 20, 0);

    WorldImage r4 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r4, 0, 20);

    WorldImage r5 = new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r5, 0, 20);

    WorldImage r6 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r6, 20, 20);

    WorldImage r7 = new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r7, 20, 20);

    WorldImage r8 = new RectangleImage(17, 17, OutlineMode.SOLID, Color.green).movePinhole(9, 9);
    background.placeImageXY(r8, 0, 0);

    WorldImage r9 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.cyan).movePinhole(-10, -10);
    background.placeImageXY(r9, 0, 0);

    WorldImage r10 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow).movePinhole(-10,
        -10);
    background.placeImageXY(r10, 20, 20);

    WorldImage r11 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r11, 20, 0);

    WorldImage r12 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r12, 0, 20);

    WorldImage r13 = new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r13, 0, 20);

    WorldImage r14 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r14, 20, 20);

    WorldImage r15 = new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r15, 20, 20);

    WorldImage r16 = new RectangleImage(17, 17, OutlineMode.SOLID, Color.magenta).movePinhole(9, 9);
    background.placeImageXY(r16, 0, 0);

    t.checkExpect(maze2.drawBoard(), background);
  }

  // test for makeScene()
  void testMakeScene(Tester t) {
    MazeWorld maze2 = new MazeWorld();

    WorldScene background = new WorldScene(0, 0);

    background.placeImageXY(new RectangleImage(0, 0, OutlineMode.OUTLINE, Color.black), 0, 0);

    WorldImage r1 = new TextImage("Time remaining: 1", 15, Color.blue);
    background.placeImageXY(r1, 30, 30);

    WorldImage r2 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow).movePinhole(-11,
        -11);
    background.placeImageXY(r2, 0, 0);

    WorldImage r3 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.magenta).movePinhole(-11,
        -11);
    background.placeImageXY(r3, 20, 20);

    WorldImage r4 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r4, 20, 0);

    WorldImage r5 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r5, 0, 20);

    WorldImage r6 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r6, 0, 20);

    WorldImage r7 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r7, 20, 20);

    WorldImage r8 = new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r8, 20, 20);

    WorldImage r9 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.magenta).movePinhole(-11,
        -11);
    background.placeImageXY(r9, 0, 0);

    WorldImage r10 = new TextImage("Time remaining: 1", 15, Color.blue);
    background.placeImageXY(r10, 100, 220);

    t.checkExpect(maze2.makeScene(), background);
  }

  // tests for mover(String)
  void testMover(Tester t) {

    t.checkExpect(p1.mover("up"), false);
    t.checkExpect(p2.mover("right"), false);
    t.checkExpect(p3.mover("bottom"), false);
    t.checkExpect(p4.mover("left"), false);
  }

  // tests for onKeyReleased(String)
  void testOnKeyReleased(Tester t) {

    MazeWorld maze2 = new MazeWorld();

    WorldScene background = new WorldScene(0, 0);

    background.placeImageXY(new RectangleImage(0, 0, OutlineMode.OUTLINE, Color.black), 0, 0);

    WorldImage r1 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.pink).movePinhole(-10, -10);
    background.placeImageXY(r1, 0, 0);

    WorldImage r2 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.cyan).movePinhole(-10, -10);
    background.placeImageXY(r2, 20, 20);

    WorldImage r3 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r3, 20, 0);

    WorldImage r4 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r4, 0, 20);

    WorldImage r5 = new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r5, 0, 20);

    WorldImage r6 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r6, 20, 20);

    WorldImage r7 = new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r7, 20, 20);

    WorldImage r8 = new RectangleImage(17, 17, OutlineMode.SOLID, Color.green).movePinhole(9, 9);
    background.placeImageXY(r8, 0, 0);

    WorldImage r9 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.cyan).movePinhole(-10, -10);
    background.placeImageXY(r9, 0, 0);

    WorldImage r10 = new RectangleImage(20, 20, OutlineMode.SOLID, Color.yellow).movePinhole(-10,
        -10);
    background.placeImageXY(r10, 20, 20);

    WorldImage r11 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r11, 20, 0);

    WorldImage r12 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r12, 0, 20);

    WorldImage r13 = new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r13, 0, 20);

    WorldImage r14 = new LineImage(new Posn(0, 20), Color.black).movePinhole(-20, -10);
    background.placeImageXY(r14, 20, 20);

    WorldImage r15 = new LineImage(new Posn(20, 0), Color.black).movePinhole(-10, -20);
    background.placeImageXY(r15, 20, 20);

    WorldImage r16 = new RectangleImage(17, 17, OutlineMode.SOLID, Color.magenta).movePinhole(9, 9);
    background.placeImageXY(r16, 0, 0);

    t.checkExpect(maze2.drawBoard(), background);

    maze.onKeyReleased("r");
    maze2.onKeyReleased("up");

    t.checkExpect(maze2.drawBoard(), background);

    maze2.onKeyReleased("right");

    t.checkExpect(maze2.drawBoard(), background);

    maze2.onKeyReleased("down");

    t.checkExpect(maze2.drawBoard(), background);

    maze2.onKeyReleased("left");

    t.checkExpect(maze2.drawBoard(), background);

    maze2.onKeyReleased("b");

    t.checkExpect(maze2.drawBoard(), background);

    maze2.onKeyReleased("d");

    t.checkExpect(maze2.drawBoard(), background);

  }

  //test the size method
  void testSize(Tester t) {
    this.initDeque();
    t.checkExpect(this.emptyDeque.size(), 0);
    t.checkExpect(this.deque1.size(), 4);
    t.checkExpect(this.deque2.size(), 7);
  }

  // test the addAtHead method
  void testAddAtHead(Tester t) {
    this.initDeque();
    this.dequeTest1.addAtHead("def");
    this.dequeTest1.addAtHead("cde");
    this.dequeTest1.addAtHead("bcd");
    this.dequeTest1.addAtHead("abc");
    t.checkExpect(this.dequeTest1, this.deque1);

    this.dequeTest2.addAtHead("rocket");
    this.dequeTest2.addAtHead("boat");
    this.dequeTest2.addAtHead("plane");
    this.dequeTest2.addAtHead("bus");
    this.dequeTest2.addAtHead("truck");
    this.dequeTest2.addAtHead("bike");
    this.dequeTest2.addAtHead("car");
    t.checkExpect(this.dequeTest2, this.deque2);

  }

  // test the addAtTail method
  void testAddAtTail(Tester t) {
    this.initDeque();
    this.dequeTest1.addAtTail("abc");
    this.dequeTest1.addAtTail("bcd");
    this.dequeTest1.addAtTail("cde");
    this.dequeTest1.addAtTail("def");
    t.checkExpect(this.dequeTest1, this.deque1);

    this.dequeTest2.addAtTail("car");
    this.dequeTest2.addAtTail("bike");
    this.dequeTest2.addAtTail("truck");
    this.dequeTest2.addAtTail("bus");
    this.dequeTest2.addAtTail("plane");
    this.dequeTest2.addAtTail("boat");
    this.dequeTest2.addAtTail("rocket");
    t.checkExpect(this.dequeTest2, this.deque2);
  }

  // test the removeHelper method
  void testRemovehelper(Tester t) {
    this.initDeque();
    t.checkExpect(this.nodeABC.removeHelper(), "abc");
    t.checkExpect(this.nodeDEF.removeHelper(), "def");
    t.checkExpect(this.nodeBus.removeHelper(), "bus");
    t.checkExpect(this.nodeTruck.removeHelper(), "truck");
  }

  // test the removeFromTail method
  void testRemoveFromTail(Tester t) {
    this.initDeque();
    t.checkExpect(this.deque1.removeFromTail(), "def");
    t.checkExpect(this.deque2.removeFromTail(), "rocket");
  }

  // test the removeFromHead method
  void testRemoveFromHead(Tester t) {
    this.initDeque();
    t.checkExpect(this.deque1.removeFromHead(), "abc");
    t.checkExpect(this.deque2.removeFromHead(), "car");
  }

  // test the exceptions
  void testException(Tester t) {
    this.initDeque();
    t.checkExceptionType(RuntimeException.class, this.sentinel1, "removeHelper");

    t.checkConstructorException(new IllegalArgumentException("Null Node"), "Node", "empty",
        this.nodeBCD, null);

  }

  // initializes the world
  void testBigBang(Tester t) {
    this.maze.bigBang(this.maze.x * 20 + 20, this.maze.y * 20 + 60, 0.01);
  }
}
