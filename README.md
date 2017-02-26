# LINEbot-on-heroku
heroku上で動くLINEのbotアカウントの中身

## Description
[これ](https://github.com/ahuglajbclajep/LINEbot-SpringBoot-on-heroku) の軽量版, [SprigBoot](https://github.com/spring-projects/spring-boot) やら [MessagingAPI](https://github.com/line/line-bot-sdk-java) がしてることを自分で実装  
要らない機能を削ってるのでだいぶ速い

![demo](https://github.com/ahuglajbclajep/LINEbot-on-heroku/blob/pic/README.gif)

## Install
### Create bot account
[ここ](https://business.line.me/ja/services/bot)で作る   
細かな手順は割愛、一番面倒なとこ

### Deploy to Heroku
下のボタンを押し、[ここ](https://github.com/line/line-bot-sdk-java/blob/master/sample-spring-boot-echo/README.md)を参考に設定  
**App Name は必ず設定すること**

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/ahuglajbclajep/LINEbot-on-heroku)

## Examples Of Command
### @qr [string]
```
@qr https://github.com/ahuglajbclajep/LINEbot-on-heroku
```
QRコードを生成する  
他のコマンドで作ったURLを共有するのにも使える

### @wol [question]
```
@wol graph Mickey Mouse curve
```  
[WolframAlpha](http://www.wolframalpha.com)で検索する  
計算したり天気聞いたり

### @twt [keyword]
```
@twt 睦月
```  
[Twitter](https://twitter.com)を検索する。~~この機能いる？~~

その他画像やスタンプにも反応します _*ケッコンカッコカリは準備中*_

## Future Releases
* 指定したタイムゾーンの現在時刻を返す`@time`コマンドの実装
* 発言をランダムにする

## Contribution
1. Fork it  
2. Create your feature branch  
3. Commit your changes  
4. Push to the branch  
5. Create new Pull Request

## Licence
[MIT](LICENSE)