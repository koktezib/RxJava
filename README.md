# Отчёт по реализации мини-RxJava

## Введение

**Цель работы:** реализовать базовые концепции реактивного программирования.

Краткое описание компонентов:  
- **Observable**  
- **Observer**  
- **Disposable**  
- **Schedulers**  
- **Операторы**

---

## Архитектура системы

### Схема (диаграмма классов) основных компонентов:

- **Observable<T>**: хранит `OnSubscribe<T>`.
- **Observer<T>**: интерфейс, реализуемый потребителем.
- **Disposable**: механизм отмены подписки.
- **Scheduler**: интерфейс + реализации.

**Принцип «цепочки» операторов:** каждый оператор возвращает новый Observable.

**Как устроена цепочка вызовов:**  
`observable.map(...).filter(...).subscribe(...)` — каждый оператор оборачивает предыдущий Observable, формируя пайплайн.

---

## Описание базовых компонентов

### Observer<T> и Disposable

- **Observer<T>** — получает события:  
  - `onNext(T item)`  
  - `onError(Throwable t)`  
  - `onComplete()`
- **Disposable** — позволяет отменить подписку и прекратить получение событий.

---

### Observable<T>

- **create(OnSubscribe):** инициализация с фабричной функцией OnSubscribe.
- **Что хранится внутри:** ссылка на OnSubscribe.
- **Механизм подписки:**  
  - `subscribe(observer)` вызывает `onSubscribe.subscribe(observer)`.
  - Защита от множественных вызовов после dispose: после вызова `dispose()` события не пересылаются.

---

### OnSubscribe<T>

- **Функциональный интерфейс:** принимает Observer, описывает, как генерировать события.
- **Пример:**
    ```java
    Observable<Integer> observable = Observable.create(obs -> {
        obs.onNext(1);
        obs.onNext(2);
        obs.onComplete();
    });
    ```
  Здесь пользователь сам генерирует события для Observer.

---

## Операторы преобразования

### map

- **Прокси-Observer:** принимает значения от upstream, применяет функцию mapper и передаёт downstream.
- Ошибки, возникающие при вызове `mapper.apply(...)`, перехватываются и передаются в `onError`.
- **Пример:**
    ```java
    Operators.map(src, x -> x * 2)
    ```

---

### filter

- **Прокси-Observer:** пропускает downstream только те значения, которые удовлетворяют предикату.
- **Пример:**
    ```java
    Operators.filter(src, x -> x > 5)
    ```

---

### flatMap

- На каждый элемент T вызывается `mapper.apply(T)`, возвращающий Observable<R>.
- На каждый такой Observable<R> подписываемся, пересылаем все onNext(R) в downstream.
- `onComplete()` вызывается только после завершения исходного Observable и всех внутренних Observable.
- Важно хранить состояние sourceCompleted и коллекцию внутренних Disposable.
- Если любой внутренний Observable вызывает onError, вся цепочка завершается ошибкой.

---

## Scheduler и управление потоками

**Назначение Scheduler:** отделить генерацию данных (I/O, вычисления) от их обработки (например, UI).

- **Scheduler (интерфейс):**
    ```java
    void execute(Runnable task);
    ```
- **IOThreadScheduler:**  
  Использует Executors.newCachedThreadPool().  
  + Подстраивается под нагрузку  
  - Нет лимита на количество потоков

- **ComputationScheduler:**  
  Executors.newFixedThreadPool(N), где N = число ядер CPU.  
  Предназначен для вычислений без I/O.

- **SingleThreadScheduler:**  
  Executors.newSingleThreadExecutor().  
  Для последовательного исполнения задач (например, UI).

---

### subscribeOn / observeOn

- **subscribeOn:** выполнение всей логики подписки и генерации событий переносится в указанный Scheduler.
- **observeOn:** каждое событие onNext/onError/onComplete передаётся в указанный Scheduler (например, для доставки в UI).
- **Пример:**
    ```java
    Observable.create(...)
        .subscribeOn(io)
        .map(...)
        .observeOn(single)
        .filter(...)
        .subscribe(obs);
    ```
- Если вызвать несколько subscribeOn — работает только первый; observeOn — действует каждый по очереди.

---

## Управление подписками

- **Disposable:** возвращается при subscribe(), позволяет отписаться.
- **Зачем dispose():** после вызова dispose события не должны больше поступать.
- Для flatMap — dispose должен отменять исходную и все внутренние подписки.
- Продвинутая реализация: хранить коллекцию Disposable для всех вложенных Observable и отменять их все при dispose.

---

## Примеры использования библиотеки

### Пример 1 (простая цепочка):

```java
Observable<Integer> src = Observable.create(obs -> {
    for (int i = 1; i <= 5; i++) {
        obs.onNext(i);
    }
    obs.onComplete();
});

Operators.map(src, x -> x * 2)
         .filter(x -> x > 5)
         .subscribe(new Observer<Integer>() {
             @Override public void onNext(Integer i) { System.out.println("Получили " + i); }
             @Override public void onError(Throwable t) { t.printStackTrace(); }
             @Override public void onComplete() { System.out.println("Конец"); }
         });
```
### Пример 2 (flatMap + параллельная обработка):


```java
Observable<Integer> src = Observable.create(obs -> {
    for (int i = 1; i <= 3; i++) obs.onNext(i);
    obs.onComplete();
});

Operators.flatMap(src, n -> 
    Observable.create(innerObs -> {
        // Симуляция асинхронной работы (например, запрос к сети)
        new Thread(() -> {
            try { Thread.sleep(100 * n); } catch (InterruptedException ignored) {}
            innerObs.onNext(n * 10);
            innerObs.onComplete();
        }).start();
    })
)
.subscribeOn(new IOThreadScheduler())
.observeOn(new SingleThreadScheduler())
.subscribe(new Observer<Integer>() {
    @Override public void onNext(Integer item) { System.out.println("Got: " + item); }
    @Override public void onError(Throwable t) { t.printStackTrace(); }
    @Override public void onComplete() { System.out.println("Done"); }
});
```

## Тестирование

**Стратегия тестирования:**  
Используется подход белого ящика с написанием юнит-тестов для всех ключевых компонентов библиотеки.  
Покрываются следующие аспекты:

- **Юнит-тесты операторов:**
    - Проверка работы `map` (правильное преобразование каждого значения).
    - Проверка работы `filter` (фильтрация элементов по условию).
    - Проверка работы `flatMap` (разворачивание вложенных Observable и корректная пересылка всех событий downstream).

- **Тесты Schedulers:**
    - Проверка, что `subscribeOn` действительно переносит выполнение подписки в отдельный поток (например, I/O-пул).
    - Проверка, что `observeOn` меняет поток исполнения onNext/onError/onComplete (например, на computation-пул или single-thread scheduler).
    - Проверка последовательности исполнения задач в single-thread scheduler.

- **Тесты на отмену подписки (`Disposable`):**
    - Проверка, что после вызова `dispose()` события больше не поступают в Observer.
    - Проверка, что dispose работает корректно даже для вложенных подписок (например, внутри `flatMap`).

- **Тест “бесконечного Observable”:**
    - Запуск Observable с бесконечной эмиссией элементов.
    - Отписка (`dispose()`) через небольшое время.
    - Проверка, что после отписки новые элементы не приходят в Observer.

**Все тесты выполняются с использованием блокировки потоков (CountDownLatch) для синхронизации асинхронных сценариев и проверки корректного завершения подписки и потоков.**


