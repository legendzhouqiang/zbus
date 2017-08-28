﻿using System;
using System.Collections.Generic;

namespace Zbus.Kit.Json
{
    internal class Getters
    {
        public string Name;
        public JSON.GenericGetter Getter;
        public Type propertyType;
    }

    public class DatasetSchema
    {
        public List<string> Info { get; set; }
        public string Name { get; set; }
    }
}
