# Android Transformer

![Logo](assets/logo.png)

Android Transformer It's Java library to manage your object transformations between your POJO objects.

## Version

[ ![Download](https://api.bintray.com/packages/txusballesteros/maven/android-transformer/images/download.svg) ](https://bintray.com/txusballesteros/maven/android-transformer/_latestVersion)

## How to use

Add the library dependency to your build.gradle file.
```java
dependencies {
    ...
    compile 'com.mobandme:android-transformer:1.0.0'
}
```

Use @Mappable annotation on your class definitions and @Mapped on the fields that you want map to the destination object. See that you destination object type
is defined by the 'with' configuration on @Mappable annotation.

IMPORTANT, You don't need configure nothing on your destination classes because the library made this job to you.

```java
@Mappable( with = Home.class )
class HomeModel {

    @Mapped
    public String Address;
    
    // If your objects does not contains the same names into their fields,
    // you can configure the destiny name using the 'toField' parameter.
    @Mapped ( toField = "PostalCode")
    public String CityCode;
    ...
}
```

```java
class Home {

    public String Address;
    
    public String PostalCode;
    ...
}
```
And now you will can make your object conversion.

```java
public class MainActivity extends Activity {
    
    private void MyMethod(HomeModel model) {
        //Build your Class transformer.
        Transformer homeModelTransformer = new Transformer
                                                    .Builder()
                                                        .build(HomeModel.class);

        ...
        
        //Converting your Model objects to your Domain objects.
        Home home = (Home)homeModelTransformer.transform(model);
        
        //If you don't like make castings between your objects, you can use this.
        Home home = homeModelTransformer.transform(model, Home.class);
        
        ...
        
        //Converting your Domain objects to your Model objects.
        HomeModel homeModel = (HomeModel)homeModelTransformer.transform(home);
        
        //If you don't like make castings between your objects, you can use this.
        HomeModel homeModel = homeModelTransformer.transform(home, HomeModel.class);
        ...
    }
}
```

## License

Copyright Txus Ballesteros 2015 (@txusballesteros)

This file is part of some open source application.

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 
Contact: Txus Ballesteros <txus.ballesteros@gmail.com>
