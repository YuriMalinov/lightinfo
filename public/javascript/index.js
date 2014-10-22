var main = angular.module("Main", []);

main.controller("MainCtrl", function () {
    this.value = 123;
    this.infos = [
        {id: 10, name: "Some info", keywords: "some info keyword word", childrenCount: 0},
        {id: 12, name: "Another info", keywords: "Info keywords", childrenCount: 10},
        {id: 10, name: "This is name", keywords: "words", childrenCount: 1}

    ]
});