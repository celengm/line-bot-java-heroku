# LINEbot-on-heroku
heroku上で動くLINEのbotアカウントの中身

## 説明
Javaの講義の最終課題で作ったので~~パフォーマンスを犠牲にして~~Javaで書いてます。  
気味が悪いほどコメントあるのもそのせい。

*お使いの睦月は正常にゃしぃ！いひひっ！*

## 使い方
### bot用のアカウントを作る
[ここ](https://business.line.me/ja/services/bot)で作る。  
細かな手順は割愛、一番面倒なとこ。

### 設定とか
[LINE@ Manager](https://admin-official.line.me) でbotとして使うための設定を終えたら  
```
heroku create hoge
```  
でheroku上にアプリケーションを作り  
出力されたURLを [LINE developers](https://developers.line.me/ba) のWebhook URLのところに貼る。  
Channel Secret、Channel Access Token もコピーして  
[これ](https://github.com/ahuglajbclajep/LINEbot-on-heroku/blob/master/src/main/java/mutuki/Send.java)の   
```
SECRET_KEY = "Channel Secret";
TOKEN = "Channel Access Token";
APP_NAME = "Webhook URL";
```  
をそれぞれ置き換える。**コンパイルは不要。**

### デプロイする
変更をcommitしたのち  
```
git push heroku master
```  
でデプロイ。heroku上の[Maven](https://maven.apache.org)でコンパイルが始まる。

## コマンド
いくつか用意してみました。

### @wol
```
@wol graph Mickey Mouse curve
```  
[WolframAlpha](http://www.wolframalpha.com)で検索する。  
計算したり天気聞いたり。

### @twt
```
@twt 睦月
```  
[Twitter](https://twitter.com)を検索する。~~この機能いる？~~

### @qr
```
@qr https://github.com/ahuglajbclajep/LINEbot-on-heroku
```  
QRコードを生成する。他のコマンドで作ったURLを共有するのにもいい。

その他画像やスタンプにも反応します。*ケッコンカッコカリ*は準備中。

## プルリクとか質問とか
初めてなので拙いところは多々あると思いますが、そこも含めてプルリク&質問は大歓迎です！

## ライセンス
[MIT](https://github.com/ahuglajbclajep/LINEbot-on-heroku/blob/master/LICENSE)