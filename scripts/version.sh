#!/bin/bash

command -v git >/dev/null ||
{
    echo ""
    exit 1
}

git describe --tags --exact-match 2>/dev/null ||
{
    echo ""
    exit 0
}