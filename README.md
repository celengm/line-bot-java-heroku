# LINEbot-on-heroku
heroku上で動くLINEのbotアカウントの中身

## Description
Javaの講義の最終課題で作ったのでJavaで書いてます。  
~~気味が悪いほどコメントあるのもそのせい。~~

![pic](https://github.com/ahuglajbclajep/LINEbot-on-heroku/blob/pic/README.gif)

## Usage
### Create bot account
[ここ](https://business.line.me/ja/services/bot)で作る。  
細かな手順は割愛、一番面倒なとこ。

### Config
[LINE@ Manager](https://admin-official.line.me) でbotとして使うための設定を終えたら  
```
heroku create [appname]
```  
でheroku上にアプリケーションを作り  
出力されたURLを [LINE developers](https://developers.line.me/ba) の*Webhook URL*のところに貼る。  
*Channel Secret*、*Channel Access Token*もコピーして  
[これ](src/main/java/mutuki/Send.java)の   
```java
private static final String SECRET_KEY = "Channel Secret";
private static final String TOKEN = "Channel Access Token";
private static final String APP_NAME = "Webhook URL";
```  
をそれぞれ置き換える。**コンパイルは不要。**

### Deploy to Heroku
変更をcommitしたのち  
```
git push heroku master
```  
でデプロイ。heroku上の[Maven](https://maven.apache.org)でコンパイルが始まる。

## Examples of Command
いくつか用意してみました。

### @wol [question]
```
@wol graph Mickey Mouse curve
```  
[WolframAlpha](http://www.wolframalpha.com)で検索する。  
計算したり天気聞いたり。

### @twt [keyword]
```
@twt 睦月
```  
[Twitter](https://twitter.com)を検索する。~~この機能いる？~~

### @qr [string]
```
@qr https://github.com/ahuglajbclajep/LINEbot-on-heroku
```  
QRコードを生成する。他のコマンドで作ったURLを共有するのにもいい。

その他画像やスタンプにも反応します。*ケッコンカッコカリは準備中。*

## Pull Requests & Questions
初めてなので拙いところは多々あると思いますが、そこも含めてプルリク&質問は大歓迎です！

## Licence
[MIT](LICENSE)