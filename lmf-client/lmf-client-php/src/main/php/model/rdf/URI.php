<?php
/*
 * Copyright (C) 2013 Salzburg Research.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
namespace LMFClient\Model\RDF;

require_once 'RDFNode.php';
use LMFClient\Model\RDF\RDFNode;

/**
 * Represents a RDF URI Resource in PHP.
 *
 * User: sschaffe
 * Date: 25.01.12
 * Time: 10:13
 * To change this template use File | Settings | File Templates.
 */
class URI extends RDFNode
{
    /** @var URI of the URI Resource (string) */
    private $uri;

    function __construct($uri)
    {
        $this->uri = $uri;
    }



    function __toString()
    {
        return $this->uri;
    }

    public function getUri()
    {
        return $this->uri;
    }


}